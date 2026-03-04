package id.ac.ui.cs.advprog.backend.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
Tanggung jawab: aturan bisnis auth (register/login/refresh/logout + enforce session limit).
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties props;
    private final Clock clock;

    public AuthService(
            final UserRepository userRepository,
            final SessionRepository sessionRepository,
            final PasswordEncoder passwordEncoder,
            final AuthProperties props
    ) {
        this(userRepository, sessionRepository, passwordEncoder, props, Clock.systemUTC());
    }

    // visible for tests
    AuthService(
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
    }

    @Transactional
    public void register(final String username, final String password) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw AuthException.usernameTaken();
        }
        final String hash = passwordEncoder.encode(password);
        userRepository.insert(username, hash, Role.BUYER);
    }

    @Transactional
    public SessionRepository.TokenPair login(
            final String username,
            final String password,
            final String userAgent,
            final String ip
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
                userAgent,
                ip
        );
    }

    @Transactional
    public SessionRepository.TokenPair refresh(
            final String refreshToken,
            final String userAgent,
            final String ip
    ) {
        final Instant now = Instant.now(clock);
        final Duration accessTtl = Duration.ofMinutes(props.getAccessTtlMinutes());
        final Duration refreshTtl = Duration.ofDays(props.getRefreshTtlDays());

        return sessionRepository
                .rotateByRefreshToken(refreshToken, now, now.plus(accessTtl), now.plus(refreshTtl), userAgent, ip)
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

        if (props.getOverflowPolicy() == AuthProperties.SessionOverflowPolicy.REJECT) {
            throw AuthException.tooManySessions();
        }

        // revoke the oldest sessions so we have room for a new one
        sessionRepository.revokeOldestSessions(userId, (active - max) + 1, now);
    }
}