package id.ac.ui.cs.advprog.backend.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/*
Tanggung jawab: aturan bisnis auth (register/login/refresh/logout + enforce session limit).
 */
@Service
public class AuthService {

    public record ClientMeta(String userAgent, String ip) {}

    public sealed interface LoginResult permits LoginResult.Tokens, LoginResult.MfaRequired {
        record Tokens(SessionRepository.TokenPair tokens) implements LoginResult {}
        record MfaRequired(UUID challengeId, String method, long expiresInSeconds, String devCode) implements LoginResult {}
    }

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties props;
    private final Clock clock;

    private final AuthProperties.SessionOverflowPolicy overflowPolicy;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthService.class);

    private final EmailVerificationRepository emailVerificationRepository;
    private final MfaChallengeRepository mfaChallengeRepository;

    @SuppressWarnings("PMD.LawOfDemeter")
    public AuthService(
            final UserRepository userRepository,
            final SessionRepository sessionRepository,
            final PasswordEncoder passwordEncoder,
            final AuthProperties props,
            final Clock clock,
            final EmailVerificationRepository emailVerificationRepository,
            final MfaChallengeRepository mfaChallengeRepository
    ) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
        this.clock = clock;
        this.overflowPolicy = props.getOverflowPolicy();
        this.emailVerificationRepository = emailVerificationRepository;
        this.mfaChallengeRepository = mfaChallengeRepository;
    }

    @Transactional
    public UUID register(final String username, final String password, final Role role) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw AuthException.usernameTaken();
        }

        final String hash = passwordEncoder.encode(password);

        final Role safeRole = (role == null) ? Role.BUYER : role;
        if (safeRole == Role.ADMIN) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "invalid_role");
        }

        final long userId = userRepository.insert(username, hash, safeRole);

        final Instant now = Instant.now(clock);
        final UUID token = emailVerificationRepository.createToken(userId, now, now.plus(Duration.ofHours(24)));

        log.info("DEV EMAIL VERIFY token for {}: {}", username, token);
        return token;
    }

    @Transactional
    public void verifyEmail(final String tokenString) {
        final UUID token;
        try {
            token = UUID.fromString(tokenString.trim());
        } catch (Exception e) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "invalid_token");
        }

        final Instant now = Instant.now(clock);
        final long userId = emailVerificationRepository.consumeIfValid(token, now)
                .orElseThrow(() -> new AuthException(HttpStatus.BAD_REQUEST, "invalid_or_expired_token"));

        userRepository.setEmailVerified(userId, true);
    }

    @Transactional
    public LoginResult login(final String username, final String password, final ClientMeta meta) {
        final var user = userRepository.findByUsername(username)
                .orElseThrow(AuthException::invalidCredentials);

        if (user.disabled()) throw AuthException.userDisabled();
        if (!user.emailVerified()) throw AuthException.emailNotVerified();

        if (!passwordEncoder.matches(password, user.passwordHash())) {
            throw AuthException.invalidCredentials();
        }

        final Instant now = Instant.now(clock);
        enforceConcurrentSessionLimit(user.id(), now);

        if (user.mfaEnabled()) {
            final String method = (user.mfaMethod() == null || user.mfaMethod().isBlank())
                    ? "EMAIL"
                    : user.mfaMethod().trim().toUpperCase(Locale.ROOT);

            if (!"EMAIL".equals(method)) {
                throw new AuthException(HttpStatus.NOT_IMPLEMENTED, "mfa_method_not_supported");
            }

            final String code = generate6DigitCode();
            final String hash = passwordEncoder.encode(code);
            final UUID challengeId = mfaChallengeRepository.createEmailChallenge(user.id(), hash, now.plusSeconds(300));

            log.info("DEV MFA EMAIL code for {}: {} (challengeId={})", username, code, challengeId);

            return new LoginResult.MfaRequired(challengeId, "EMAIL", 300, code);
        }

        return new LoginResult.Tokens(issueTokens(user.id(), now, meta));
    }

    @Transactional
    public SessionRepository.TokenPair verifyMfa(final String challengeIdStr, final String code, final ClientMeta meta) {
        final UUID id;
        try {
            id = UUID.fromString(challengeIdStr.trim());
        } catch (Exception e) {
            throw AuthException.invalidMfaChallenge();
        }

        final Instant now = Instant.now(clock);
        final var row = mfaChallengeRepository.findById(id).orElseThrow(AuthException::invalidMfaChallenge);

        if (row.usedAt() != null) throw AuthException.invalidMfaChallenge();
        if (row.expiresAt() == null || row.expiresAt().toInstant().isBefore(now)) throw AuthException.invalidMfaChallenge();
        if (row.attempts() >= 5) throw AuthException.mfaTooManyAttempts();

        if (code == null || code.isBlank() || !passwordEncoder.matches(code.trim(), row.codeHash())) {
            mfaChallengeRepository.incrementAttempts(id);
            throw AuthException.invalidMfaCode();
        }

        mfaChallengeRepository.markUsed(id, now);

        enforceConcurrentSessionLimit(row.userId(), now);
        return issueTokens(row.userId(), now, meta);
    }

    @Transactional
    public void enableEmailMfa(final long userId) {
        userRepository.setMfa(userId, true, "EMAIL");
    }

    @Transactional
    public void disableMfa(final long userId) {
        userRepository.setMfa(userId, false, null);
    }

    @Transactional
    public SessionRepository.TokenPair refresh(final String refreshToken, final ClientMeta meta) {
        final Instant now = Instant.now(clock);
        final Duration accessTtl = Duration.ofMinutes(props.getAccessTtlMinutes());
        final Duration refreshTtl = Duration.ofDays(props.getRefreshTtlDays());

        return sessionRepository
                .rotateByRefreshToken(
                        refreshToken,
                        now,
                        now.plus(accessTtl),
                        now.plus(refreshTtl),
                        meta.userAgent(),
                        meta.ip()
                )
                .orElseThrow(AuthException::refreshTokenInvalid);
    }

    @Transactional
    public void logout(final String accessToken) {
        sessionRepository.revokeByAccessToken(accessToken, Instant.now(clock));
    }

    private SessionRepository.TokenPair issueTokens(final long userId, final Instant now, final ClientMeta meta) {
        final Duration accessTtl = Duration.ofMinutes(props.getAccessTtlMinutes());
        final Duration refreshTtl = Duration.ofDays(props.getRefreshTtlDays());

        return sessionRepository.create(
                userId,
                now,
                now.plus(accessTtl),
                now.plus(refreshTtl),
                meta.userAgent(),
                meta.ip()
        );
    }

    private void enforceConcurrentSessionLimit(final long userId, final Instant now) {
        final int max = props.getMaxSessionsPerUser();
        if (max <= 0) return;

        final int active = sessionRepository.countActiveSessions(userId, now);
        if (active < max) return;

        if (overflowPolicy == AuthProperties.SessionOverflowPolicy.REJECT) {
            throw AuthException.tooManySessions();
        }

        sessionRepository.revokeOldestSessions(userId, (active - max) + 1, now);
    }

    private static String generate6DigitCode() {
        final int n = (int) (Math.random() * 1_000_000);
        return String.format("%06d", n);
    }
}