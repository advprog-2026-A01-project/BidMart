package id.ac.ui.cs.advprog.backend.auth.service;

import id.ac.ui.cs.advprog.backend.auth.model.Role;
import id.ac.ui.cs.advprog.backend.auth.repository.SessionRepository;
import id.ac.ui.cs.advprog.backend.auth.repository.UserAuthRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthLoginService {

    public record ClientMeta(String userAgent, String ip) {}

    public sealed interface LoginResult permits LoginResult.Tokens, LoginResult.MfaRequired {
        record Tokens(SessionRepository.TokenPair tokens) implements LoginResult {}
        record MfaRequired(UUID challengeId, String method, long expiresInSeconds, String devCode) implements LoginResult {}
    }

    private final Clock clock;
    private final UserAuthenticator authenticator;
    private final SessionLimitService sessionLimitService;
    private final AuthTokenService tokenService;
    private final MfaChallengeService mfaChallengeService;
    private final MfaVerificationService mfaVerificationService;
    private final AuthMfaManagementService mfaManagementService;

    public AuthLoginService(
            final Clock clock,
            final UserAuthenticator authenticator,
            final SessionLimitService sessionLimitService,
            final AuthTokenService tokenService,
            final MfaChallengeService mfaChallengeService,
            final MfaVerificationService mfaVerificationService,
            final AuthMfaManagementService mfaManagementService
    ) {
        this.clock = clock;
        this.authenticator = authenticator;
        this.sessionLimitService = sessionLimitService;
        this.tokenService = tokenService;
        this.mfaChallengeService = mfaChallengeService;
        this.mfaVerificationService = mfaVerificationService;
        this.mfaManagementService = mfaManagementService;
    }

    @Transactional
    public LoginResult login(final String username, final String password, final String privateKey, final ClientMeta meta) {
        final UserAuthRepository.UserRow user = authenticator.authenticate(username, password);
        final Instant now = Instant.now(clock);

        if (usesPrivateKeyLogin(user)) {
            authenticator.verifyPersonalKey(user, privateKey);
            sessionLimitService.enforce(user.id(), now);
            final var pair = tokenService.issue(user.id(), now, new AuthTokenService.ClientMeta(meta.userAgent(), meta.ip()));
            return new LoginResult.Tokens(pair);
        }

        sessionLimitService.enforce(user.id(), now);

        if (shouldAlwaysRequireOtp(user)) {
            return mfaChallengeService.createChallenge(user, username, now);
        }

        if (!user.mfaEnabled()) {
            final var pair = tokenService.issue(user.id(), now, new AuthTokenService.ClientMeta(meta.userAgent(), meta.ip()));
            return new LoginResult.Tokens(pair);
        }

        return mfaChallengeService.createChallenge(user, username, now);
    }

    @Transactional
    public SessionRepository.TokenPair verifyMfa(final String challengeId, final String code, final ClientMeta meta) {
        final Instant now = Instant.now(clock);
        final long userId = mfaVerificationService.verifyAndConsume(challengeId, code, now);

        sessionLimitService.enforce(userId, now);
        return tokenService.issue(userId, now, new AuthTokenService.ClientMeta(meta.userAgent(), meta.ip()));
    }

    @Transactional
    public void enableEmailMfa(final long userId) {
        mfaManagementService.enableEmail(userId);
    }

    @Transactional
    public void disableMfa(final long userId) {
        mfaManagementService.disableAllMfa(userId);
    }

    private boolean shouldAlwaysRequireOtp(final UserAuthRepository.UserRow user) {
        return user.role() != null && user.role() != Role.ADMIN;
    }

    private static boolean usesPrivateKeyLogin(final UserAuthRepository.UserRow user) {
        return user.role() != null
                && user.role() != Role.ADMIN
                && user.personalKeyHash() != null
                && !user.personalKeyHash().isBlank();
    }
}