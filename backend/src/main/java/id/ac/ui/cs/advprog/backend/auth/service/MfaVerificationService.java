package id.ac.ui.cs.advprog.backend.auth.service;

import id.ac.ui.cs.advprog.backend.auth.model.AuthException;
import id.ac.ui.cs.advprog.backend.auth.model.AuthProperties;
import id.ac.ui.cs.advprog.backend.auth.repository.MfaChallengeRepository;
import id.ac.ui.cs.advprog.backend.auth.repository.UserAuthRepository;
import id.ac.ui.cs.advprog.backend.auth.util.TotpUtil;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class MfaVerificationService {

    private static final String METHOD_EMAIL = "EMAIL";
    private static final String METHOD_TOTP = "TOTP";
    private static final int DEFAULT_MAX_ATTEMPTS = 5;

    private final MfaChallengeRepository mfaChallengeRepository;
    private final UserAuthRepository userAuthRepository;
    private final PasswordEncoder passwordEncoder;
    private final int maxAttempts;

    public MfaVerificationService(
            final MfaChallengeRepository mfaChallengeRepository,
            final UserAuthRepository userAuthRepository,
            final PasswordEncoder passwordEncoder,
            final AuthProperties props
    ) {
        this.mfaChallengeRepository = mfaChallengeRepository;
        this.userAuthRepository = userAuthRepository;
        this.passwordEncoder = passwordEncoder;
        this.maxAttempts = props.getMfaMaxAttempts();
    }

    public long verifyAndConsume(final String challengeIdStr, final String code, final Instant now) {
        final UUID id = parseUuidUnauthorized(challengeIdStr, "invalid_mfa_challenge");

        final var row = mfaChallengeRepository.findById(id).orElseThrow(AuthException::invalidMfaChallenge);
        validateChallenge(row, now, maxAttempts);

        final String method = normalizeMethod(row.method());
        if (METHOD_TOTP.equals(method)) {
            final String secret = userAuthRepository.getTotpSecret(row.userId()).orElse(null);
            if (secret == null || !TotpUtil.verifyCode(secret, code, now)) {
                mfaChallengeRepository.incrementAttempts(id);
                throw AuthException.invalidMfaCode();
            }
        } else {
            validateEmailOtp(row.codeHash(), code, id);
        }

        mfaChallengeRepository.markUsed(id, now);
        return row.userId();
    }

    private void validateEmailOtp(final String storedHash, final String code, final UUID id) {
        final String safe = (code == null) ? "" : code.trim();
        if (safe.isBlank() || !passwordEncoder.matches(safe, storedHash)) {
            mfaChallengeRepository.incrementAttempts(id);
            throw AuthException.invalidMfaCode();
        }
    }

    private static void validateChallenge(final MfaChallengeRepository.Row row, final Instant now, final int maxAttemptsRaw) {
        final int max = (maxAttemptsRaw <= 0) ? DEFAULT_MAX_ATTEMPTS : maxAttemptsRaw;
        if (row.usedAt() != null) throw AuthException.invalidMfaChallenge();
        if (row.expiresAt() == null || row.expiresAt().toInstant().isBefore(now)) throw AuthException.invalidMfaChallenge();
        if (row.attempts() >= max) throw AuthException.mfaTooManyAttempts();
    }

    private static String normalizeMethod(final String raw) {
        if (raw == null || raw.isBlank()) return METHOD_EMAIL;
        return raw.trim().toUpperCase(Locale.ROOT);
    }

    private static UUID parseUuidUnauthorized(final String raw, final String code) {
        if (raw == null || raw.isBlank()) throw new AuthException(HttpStatus.UNAUTHORIZED, code);
        try { return UUID.fromString(raw.trim()); }
        catch (IllegalArgumentException ex) { throw new AuthException(HttpStatus.UNAUTHORIZED, code, ex); }
    }
}