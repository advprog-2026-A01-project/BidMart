package id.ac.ui.cs.advprog.backend.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

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

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties props;
    private final Clock clock;

    private final AuthProperties.SessionOverflowPolicy overflowPolicy;

    @SuppressWarnings("PMD.LawOfDemeter")
    public AuthService(
            final UserRepository userRepository,
            final SessionRepository sessionRepository,
            final PasswordEncoder passwordEncoder,
            final AuthProperties props,
            final Clock clock
    ) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
        this.clock = clock;
        this.overflowPolicy = props.getOverflowPolicy();
    }

    // Fungsi register() untuk tambah user baru beserta password dan rolenya
    @Transactional
    public void register(final String username, final String password, final Role role) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw AuthException.usernameTaken();
        }
        final String hash = passwordEncoder.encode(password);

        // enforce: never allow self-register as ADMIN
        final Role safeRole = (role == null) ? Role.BUYER : role;
        if (safeRole == Role.ADMIN) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "invalid_role");
        }

        userRepository.insert(username, hash, safeRole);
    }

    @Transactional
    public SessionRepository.TokenPair login(
            final String username,
            final String password,
            final ClientMeta meta
    ) {
        final var user = userRepository.findByUsername(username)
                .orElseThrow(AuthException::invalidCredentials);

        if (user.disabled()) {
            throw AuthException.userDisabled();
        }

        if (!passwordEncoder.matches(password, user.passwordHash())) {
            throw AuthException.invalidCredentials();
        }

        final Instant now = Instant.now(clock);

        enforceConcurrentSessionLimit(user.id(), now);

        final Duration accessTtl = Duration.ofMinutes(props.getAccessTtlMinutes());
        final Duration refreshTtl = Duration.ofDays(props.getRefreshTtlDays());

        return sessionRepository.create(
                user.id(),
                now,
                now.plus(accessTtl),
                now.plus(refreshTtl),
                meta.userAgent(),
                meta.ip()
        );
    }

    @Transactional
    public SessionRepository.TokenPair refresh(
            final String refreshToken,
            final ClientMeta meta
    ) {
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
}