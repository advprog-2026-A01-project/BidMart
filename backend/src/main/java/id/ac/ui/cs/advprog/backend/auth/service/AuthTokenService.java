package id.ac.ui.cs.advprog.backend.auth.service;

import id.ac.ui.cs.advprog.backend.auth.model.AuthError;
import id.ac.ui.cs.advprog.backend.auth.model.AuthException;
import id.ac.ui.cs.advprog.backend.auth.model.AuthProperties;
import id.ac.ui.cs.advprog.backend.auth.repository.SessionRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthTokenService {

    public record ClientMeta(String userAgent, String ip) {}

    private final SessionRepository sessionRepository;
    private final AuthProperties props;
    private final Clock clock;

    public AuthTokenService(final SessionRepository sessionRepository, final AuthProperties props, final Clock clock) {
        this.sessionRepository = sessionRepository;
        this.props = props;
        this.clock = clock;
    }

    @Transactional
    public SessionRepository.TokenPair issue(final long userId, final Instant now, final ClientMeta meta) {
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

    @Transactional
    public SessionRepository.TokenPair refresh(final String refreshToken, final ClientMeta meta) {
        final Instant now = Instant.now(clock);
        final Duration accessTtl = Duration.ofMinutes(props.getAccessTtlMinutes());
        final Duration refreshTtl = Duration.ofDays(props.getRefreshTtlDays());

        return sessionRepository.rotateByRefreshToken(
                refreshToken,
                now,
                now.plus(accessTtl),
                now.plus(refreshTtl),
                meta.userAgent(),
                meta.ip()
        ).orElseThrow(() -> AuthException.of(AuthError.INVALID_REFRESH_TOKEN));
    }

    @Transactional
    public void logout(final String accessToken) {
        sessionRepository.revokeByAccessToken(accessToken, Instant.now(clock));
    }
}