package id.ac.ui.cs.advprog.backend.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import id.ac.ui.cs.advprog.backend.auth.model.AuthException;
import id.ac.ui.cs.advprog.backend.auth.model.AuthProperties;
import id.ac.ui.cs.advprog.backend.auth.model.Role;
import id.ac.ui.cs.advprog.backend.auth.repository.MfaChallengeRepository;
import id.ac.ui.cs.advprog.backend.auth.repository.SessionRepository;
import id.ac.ui.cs.advprog.backend.auth.repository.UserAuthRepository;
import id.ac.ui.cs.advprog.backend.auth.service.AuthLoginService;
import id.ac.ui.cs.advprog.backend.auth.service.AuthMfaManagementService;
import id.ac.ui.cs.advprog.backend.auth.service.AuthTokenService;
import id.ac.ui.cs.advprog.backend.auth.service.MfaChallengeService;
import id.ac.ui.cs.advprog.backend.auth.service.MfaVerificationService;
import id.ac.ui.cs.advprog.backend.auth.service.SessionLimitService;
import id.ac.ui.cs.advprog.backend.auth.service.UserAuthenticator;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthLoginServiceTest {

    private UserAuthRepository userAuthRepository;
    private SessionRepository sessionRepository;
    private PasswordEncoder passwordEncoder;
    private MfaChallengeRepository mfaChallengeRepository;
    private JavaMailSender mailSender;

    private Clock clock;
    private AuthLoginService loginService;

    @BeforeEach
    void setUp() {
        userAuthRepository = Mockito.mock(UserAuthRepository.class);
        sessionRepository = Mockito.mock(SessionRepository.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        mfaChallengeRepository = Mockito.mock(MfaChallengeRepository.class);
        mailSender = Mockito.mock(JavaMailSender.class);

        final AuthProperties props = new AuthProperties();
        props.setMaxSessionsPerUser(10);
        props.setOverflowPolicy(AuthProperties.SessionOverflowPolicy.REVOKE_OLDEST);
        props.setMfaChallengeTtlSeconds(300);
        props.setMfaMaxAttempts(5);
        props.setAccessTtlMinutes(15);
        props.setRefreshTtlDays(7);

        clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

        final UserAuthenticator authenticator = new UserAuthenticator(userAuthRepository, passwordEncoder);
        final SessionLimitService limit = new SessionLimitService(sessionRepository, 10, AuthProperties.SessionOverflowPolicy.REVOKE_OLDEST);
        final AuthTokenService token = new AuthTokenService(sessionRepository, props, clock);
        final AuthMfaManagementService mfaManage = new AuthMfaManagementService(userAuthRepository, clock);
        final MfaChallengeService challengeService = new MfaChallengeService(mfaChallengeRepository, passwordEncoder, props, mailSender);
        final MfaVerificationService verifyService = new MfaVerificationService(mfaChallengeRepository, userAuthRepository, passwordEncoder, props);

        loginService = new AuthLoginService(clock, authenticator, limit, token, challengeService, verifyService, mfaManage);
    }

    @Test
    void smoke_login_mfa_and_verify_wrong_code() {
        // ===== Scenario A: login returns MFA required (EMAIL) =====
        final var user = new UserAuthRepository.UserRow(
                1L, "u", "hash", Role.BUYER,
                false, true, true, "EMAIL", null
        );

        when(userAuthRepository.findByUsername("u")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("p", "hash")).thenReturn(true);
        when(sessionRepository.countActiveSessions(eq(1L), any(Instant.class))).thenReturn(0);
        when(passwordEncoder.encode(any(String.class))).thenReturn("otpHash");

        final UUID challengeId = UUID.randomUUID();
        when(mfaChallengeRepository.createEmailChallenge(eq(1L), eq("otpHash"), any(Instant.class))).thenReturn(challengeId);

        final var out = loginService.login("u", "p", new AuthLoginService.ClientMeta("ua", "client"));

        final boolean loginOk = (out instanceof AuthLoginService.LoginResult.MfaRequired m)
                && challengeId.equals(m.challengeId())
                && "EMAIL".equals(m.method()); // literals-first

        // ===== Scenario B: verify MFA wrong code throws invalid_mfa_code =====
        final OffsetDateTime expiresAt = OffsetDateTime.ofInstant(clock.instant().plusSeconds(60), ZoneOffset.UTC);
        final var row = new MfaChallengeRepository.Row(challengeId, 1L, "EMAIL", "storedHash", expiresAt, null, 0);

        when(mfaChallengeRepository.findById(challengeId)).thenReturn(Optional.of(row));
        when(passwordEncoder.matches(eq("000000"), eq("storedHash"))).thenReturn(false);

        boolean verifyOk = false;
        try {
            loginService.verifyMfa(challengeId.toString(), "000000", new AuthLoginService.ClientMeta("ua", "client"));
        } catch (AuthException ex) {
            verifyOk = "invalid_mfa_code".equals(ex.getCode()); // literals-first
        }

        // NO Assertions.* calls (PMD-safe). JUnit will fail on AssertionError.
        if (!(loginOk && verifyOk)) {
            throw new AssertionError("login must require MFA and verify must reject wrong code");
        }
    }
}