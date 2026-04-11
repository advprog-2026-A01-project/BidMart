package id.ac.ui.cs.advprog.backend.auth.service;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import id.ac.ui.cs.advprog.backend.auth.model.AuthException;
import id.ac.ui.cs.advprog.backend.auth.model.AuthProperties;
import id.ac.ui.cs.advprog.backend.auth.model.Role;
import id.ac.ui.cs.advprog.backend.auth.repository.MfaChallengeRepository;
import id.ac.ui.cs.advprog.backend.auth.repository.UserAuthRepository;

@Service
public class MfaChallengeService {

    private static final String METHOD_EMAIL = "EMAIL";
    private static final String METHOD_TOTP = "TOTP";

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MfaChallengeService.class);

    private final MfaChallengeRepository mfaChallengeRepository;
    private final PasswordEncoder passwordEncoder;
    private final long ttlSeconds;
    private final EmailService emailService;
    private final AuthProperties props;

    public MfaChallengeService(
            final MfaChallengeRepository mfaChallengeRepository,
            final PasswordEncoder passwordEncoder,
            final AuthProperties props,
            final EmailService emailService
    ) {
        this.mfaChallengeRepository = mfaChallengeRepository;
        this.passwordEncoder = passwordEncoder;
        this.ttlSeconds = props.getMfaChallengeTtlSeconds();
        this.emailService = emailService;
        this.props = props;
    }

    public AuthLoginService.LoginResult.MfaRequired createChallenge(
            final UserAuthRepository.UserRow user,
            final String username,
            final Instant now
    ) {
        // BUYER / SELLER selalu dipaksa ke EMAIL OTP demo
        final String method = shouldUseDemoCode(user) ? METHOD_EMAIL : normalizeMethod(user.mfaMethod());
        final Instant expires = now.plusSeconds(ttlSeconds);

        if (METHOD_TOTP.equals(method)) {
            final String secret = (user.totpSecret() == null || user.totpSecret().isBlank()) ? null : user.totpSecret();
            if (secret == null) throw new AuthException(HttpStatus.BAD_REQUEST, "totp_not_configured");

            final UUID id = mfaChallengeRepository.createTotpChallenge(user.id(), expires);
            return new AuthLoginService.LoginResult.MfaRequired(id, METHOD_TOTP, ttlSeconds, null);
        }

        final String otp = shouldUseDemoCode(user) ? props.getDemoEmailOtpCode() : generate6DigitCode();
        final String hash = passwordEncoder.encode(otp);
        final UUID id = mfaChallengeRepository.createEmailChallenge(user.id(), hash, expires);

        log.info("DEV MFA EMAIL code for {}: {} (challengeId={})", username, otp, id);
        emailService.sendMfaOtp(username, otp);

        return new AuthLoginService.LoginResult.MfaRequired(id, METHOD_EMAIL, ttlSeconds, null);
    }

    private boolean shouldUseDemoCode(final UserAuthRepository.UserRow user) {
        return props.isDemoStaticCodesEnabled() && user.role() != null && user.role() != Role.ADMIN;
    }

    private static String normalizeMethod(final String raw) {
        if (raw == null || raw.isBlank()) return METHOD_EMAIL;
        return raw.trim().toUpperCase(Locale.ROOT);
    }

    private static String generate6DigitCode() {
        final int n = (int) (Math.random() * 1_000_000);
        return String.format(Locale.ROOT, "%06d", n);
    }
}