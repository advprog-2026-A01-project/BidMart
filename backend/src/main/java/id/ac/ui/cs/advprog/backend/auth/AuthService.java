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

@Service
@SuppressWarnings({
        "PMD.TooManyMethods",
        "PMD.GodClass"
})
public class AuthService {

    private static final String MFA_METHOD_EMAIL = "EMAIL";
    private static final String MFA_METHOD_TOTP = "TOTP";
    private static final int DEFAULT_MAX_MFA_ATTEMPTS = 5;

    public record ClientMeta(String userAgent, String ip) {}

    public sealed interface LoginResult permits LoginResult.Tokens, LoginResult.MfaRequired {
        record Tokens(SessionRepository.TokenPair tokens) implements LoginResult {}
        record MfaRequired(UUID challengeId, String method, long expiresInSeconds, String devCode) implements LoginResult {}
    }

    public record TotpSetup(String secret, String otpauthUrl) {}

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
        final UUID token = parseUuidOrBadRequest(tokenString, "invalid_token");

        final Instant now = Instant.now(clock);
        final long userId = emailVerificationRepository.consumeIfValid(token, now)
                .orElseThrow(() -> new AuthException(HttpStatus.BAD_REQUEST, "invalid_or_expired_token"));

        userRepository.setEmailVerified(userId, true);
    }

    @Transactional
    public LoginResult login(final String username, final String password, final ClientMeta meta) {
        final var user = findUserOrInvalidCredentials(username);
        validateUserLoginState(user);
        validatePasswordOrInvalidCredentials(user, password);

        final Instant now = Instant.now(clock);
        enforceConcurrentSessionLimit(user.id(), now);

        if (user.mfaEnabled()) {
            return beginMfaChallenge(user, username, now);
        }

        return new LoginResult.Tokens(issueTokens(user.id(), now, meta));
    }

    @Transactional
    public SessionRepository.TokenPair verifyMfa(final String challengeIdStr, final String code, final ClientMeta meta) {
        final UUID id = parseUuidOrInvalidMfaChallenge(challengeIdStr);

        final Instant now = Instant.now(clock);
        final var row = mfaChallengeRepository.findById(id).orElseThrow(AuthException::invalidMfaChallenge);
        validateMfaChallengeOrThrow(row, now, props.getMfaMaxAttempts());

        final String method = normalizeMfaMethod(row.method());
        if (MFA_METHOD_TOTP.equals(method)) {
            final String secret = userRepository.getTotpSecret(row.userId()).orElse(null);
            if (secret == null) {
                mfaChallengeRepository.incrementAttempts(id);
                throw AuthException.invalidMfaCode();
            }
            final boolean ok = TotpUtil.verifyCode(secret, code, now);
            if (!ok) {
                mfaChallengeRepository.incrementAttempts(id);
                throw AuthException.invalidMfaCode();
            }
        } else {
            validateMfaCodeOrThrowEmail(id, row, code);
        }

        mfaChallengeRepository.markUsed(id, now);
        enforceConcurrentSessionLimit(row.userId(), now);
        return issueTokens(row.userId(), now, meta);
    }

    @Transactional
    public void enableEmailMfa(final long userId) {
        userRepository.setMfa(userId, true, MFA_METHOD_EMAIL);
    }

    @Transactional
    public void disableMfa(final long userId) {
        userRepository.setMfa(userId, false, null);
    }

    // ===== TOTP manage =====

    @Transactional
    public TotpSetup setupTotp(final long userId, final String usernameForLabel) {
        final String secret = TotpUtil.generateBase32Secret(20);
        userRepository.setTotpSecret(userId, secret);

        final String issuer = "BidMart";
        final String account = (usernameForLabel == null || usernameForLabel.isBlank()) ? ("user-" + userId) : usernameForLabel;
        final String uri = TotpUtil.otpauthUri(issuer, account, secret);

        return new TotpSetup(secret, uri);
    }

    @Transactional
    public void enableTotp(final long userId, final String code) {
        final Instant now = Instant.now(clock);
        final String secret = userRepository.getTotpSecret(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.BAD_REQUEST, "totp_not_configured"));

        if (!TotpUtil.verifyCode(secret, code, now)) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "invalid_totp_code");
        }

        userRepository.setMfa(userId, true, MFA_METHOD_TOTP);
    }

    @Transactional
    public void disableTotp(final long userId) {
        userRepository.setMfa(userId, false, null);
        userRepository.clearTotpSecret(userId);
    }

    // ===== token lifecycle =====

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

    private UserRepository.UserRow findUserOrInvalidCredentials(final String username) {
        return userRepository.findByUsername(username).orElseThrow(AuthException::invalidCredentials);
    }

    private void validateUserLoginState(final UserRepository.UserRow user) {
        if (user.disabled()) throw AuthException.userDisabled();
        if (!user.emailVerified()) throw AuthException.emailNotVerified();
    }

    private void validatePasswordOrInvalidCredentials(final UserRepository.UserRow user, final String password) {
        final String safe = (password == null) ? "" : password;
        if (!passwordEncoder.matches(safe, user.passwordHash())) {
            throw AuthException.invalidCredentials();
        }
    }

    private LoginResult beginMfaChallenge(final UserRepository.UserRow user, final String username, final Instant now) {
        final String method = normalizeMfaMethod(user.mfaMethod());

        final long ttlSeconds = props.getMfaChallengeTtlSeconds();
        final Instant expires = now.plusSeconds(ttlSeconds);

        if (MFA_METHOD_TOTP.equals(method)) {
            // must have secret
            final String secret = (user.totpSecret() == null || user.totpSecret().isBlank()) ? null : user.totpSecret();
            if (secret == null) {
                throw new AuthException(HttpStatus.BAD_REQUEST, "totp_not_configured");
            }
            final UUID challengeId = mfaChallengeRepository.createTotpChallenge(user.id(), expires);
            return new LoginResult.MfaRequired(challengeId, MFA_METHOD_TOTP, ttlSeconds, null);
        }

        // default EMAIL
        final String code = generate6DigitCode();
        final String hash = passwordEncoder.encode(code);
        final UUID challengeId = mfaChallengeRepository.createEmailChallenge(user.id(), hash, expires);

        log.info("DEV MFA EMAIL code for {}: {} (challengeId={})", username, code, challengeId);
        return new LoginResult.MfaRequired(challengeId, MFA_METHOD_EMAIL, ttlSeconds, code);
    }

    private static String normalizeMfaMethod(final String raw) {
        if (raw == null || raw.isBlank()) return MFA_METHOD_EMAIL;
        return raw.trim().toUpperCase(Locale.ROOT);
    }

    private static UUID parseUuidOrBadRequest(final String raw, final String errorCode) {
        if (raw == null || raw.isBlank()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, errorCode);
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException ex) {
            throw new AuthException(HttpStatus.BAD_REQUEST, errorCode, ex);
        }
    }

    private static UUID parseUuidOrInvalidMfaChallenge(final String raw) {
        if (raw == null || raw.isBlank()) throw AuthException.invalidMfaChallenge();
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException ex) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "invalid_mfa_challenge", ex);
        }
    }

    private static void validateMfaChallengeOrThrow(final MfaChallengeRepository.Row row, final Instant now, final int maxAttemptsRaw) {
        final int maxAttempts = (maxAttemptsRaw <= 0) ? DEFAULT_MAX_MFA_ATTEMPTS : maxAttemptsRaw;
        if (row.usedAt() != null) throw AuthException.invalidMfaChallenge();
        if (row.expiresAt() == null || row.expiresAt().toInstant().isBefore(now)) throw AuthException.invalidMfaChallenge();
        if (row.attempts() >= maxAttempts) throw AuthException.mfaTooManyAttempts();
    }

    private void validateMfaCodeOrThrowEmail(final UUID id, final MfaChallengeRepository.Row row, final String code) {
        final String safe = (code == null) ? "" : code.trim();
        if (safe.isBlank() || !passwordEncoder.matches(safe, row.codeHash())) {
            mfaChallengeRepository.incrementAttempts(id);
            throw AuthException.invalidMfaCode();
        }
    }
}