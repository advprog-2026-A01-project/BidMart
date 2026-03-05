package id.ac.ui.cs.advprog.backend.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({
        "PMD.AvoidUsingHardCodedIP",
        "PMD.UnitTestContainsTooManyAsserts",
        "PMD.UnitTestAssertionsShouldIncludeMessage"
})
class AuthServiceTest {

    private static final String USERNAME = "user@test";
    private static final String PASSWORD = "secret";
    private static final String HASH = "hash";
    private static final String UA = "ua";
    private static final String IP = "client";

    @Mock private UserRepository userRepository;
    @Mock private SessionRepository sessionRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailVerificationRepository emailVerificationRepository;
    @Mock private MfaChallengeRepository mfaChallengeRepository;

    private AuthProperties props;
    private Clock clock;
    private AuthService service;

    @BeforeEach
    void setUp() {
        props = new AuthProperties();
        props.setMaxSessionsPerUser(10);
        props.setOverflowPolicy(AuthProperties.SessionOverflowPolicy.REVOKE_OLDEST);
        clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        service = new AuthService(
                userRepository,
                sessionRepository,
                passwordEncoder,
                props,
                clock,
                emailVerificationRepository,
                mfaChallengeRepository
        );
    }

    @Test
    void register_rejects_admin_role() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);

        final AuthException ex = assertThrows(AuthException.class, () -> service.register(USERNAME, PASSWORD, Role.ADMIN));
        assertEquals("invalid_role", ex.getCode());
    }

    @Test
    void login_returns_mfa_required_when_enabled_email() {
        final UserRepository.UserRow user = new UserRepository.UserRow(
                1L,
                USERNAME,
                HASH,
                Role.BUYER,
                false,
                true,
                true,
                null
        );

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);
        when(sessionRepository.countActiveSessions(eq(1L), any(Instant.class))).thenReturn(0);
        when(passwordEncoder.encode(any(String.class))).thenReturn("otpHash");
        final UUID challengeId = UUID.randomUUID();
        when(mfaChallengeRepository.createEmailChallenge(eq(1L), eq("otpHash"), any(Instant.class))).thenReturn(challengeId);

        final AuthService.LoginResult out = service.login(USERNAME, PASSWORD, new AuthService.ClientMeta(UA, IP));
        assertNotNull(out);

        final AuthService.LoginResult.MfaRequired mfa = (AuthService.LoginResult.MfaRequired) out;
        assertEquals(challengeId, mfa.challengeId());
        assertEquals("EMAIL", mfa.method());

        verify(sessionRepository, never()).create(anyLong(), any(Instant.class), any(Instant.class), any(Instant.class), anyString(), anyString());
    }

    @Test
    void verify_mfa_invalid_code_increments_attempts_and_throws() {
        final UUID id = UUID.randomUUID();
        final OffsetDateTime expiresAt = OffsetDateTime.ofInstant(clock.instant().plusSeconds(60), ZoneOffset.UTC);
        final MfaChallengeRepository.Row row = new MfaChallengeRepository.Row(
                id,
                7L,
                "EMAIL",
                "storedHash",
                expiresAt,
                null,
                0
        );

        when(mfaChallengeRepository.findById(id)).thenReturn(Optional.of(row));
        when(passwordEncoder.matches(eq("000000"), eq("storedHash"))).thenReturn(false);

        final AuthException ex = assertThrows(AuthException.class, () ->
                service.verifyMfa(id.toString(), "000000", new AuthService.ClientMeta(UA, IP))
        );

        assertEquals("invalid_mfa_code", ex.getCode());
        verify(mfaChallengeRepository).incrementAttempts(id);
        verify(mfaChallengeRepository, never()).markUsed(eq(id), any(Instant.class));
    }
}