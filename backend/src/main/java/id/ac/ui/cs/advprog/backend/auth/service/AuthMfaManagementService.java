package id.ac.ui.cs.advprog.backend.auth.service;

import id.ac.ui.cs.advprog.backend.auth.model.AuthException;
import id.ac.ui.cs.advprog.backend.auth.repository.UserSecurityRepository;
import id.ac.ui.cs.advprog.backend.auth.util.TotpUtil;
import java.time.Clock;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthMfaManagementService {

    private static final String METHOD_EMAIL = "EMAIL";
    private static final String METHOD_TOTP = "TOTP";

    public record TotpSetup(String secret, String otpauthUrl) {}

    private final UserSecurityRepository userSecurityRepository;
    private final Clock clock;

    public AuthMfaManagementService(final UserSecurityRepository userSecurityRepository, final Clock clock) {
        this.userSecurityRepository = userSecurityRepository;
        this.clock = clock;
    }

    @Transactional
    public void enableEmail(final long userId) {
        userSecurityRepository.setMfa(userId, true, METHOD_EMAIL);
    }

    @Transactional
    public void disableAllMfa(final long userId) {
        userSecurityRepository.setMfa(userId, false, null);
    }

    @Transactional
    public TotpSetup setupTotp(final long userId, final String usernameForLabel) {
        final String secret = TotpUtil.generateBase32Secret(20);
        userSecurityRepository.setTotpSecret(userId, secret);

        final String issuer = "BidMart";
        final String account = (usernameForLabel == null || usernameForLabel.isBlank())
                ? ("user-" + userId)
                : usernameForLabel;
        final String uri = TotpUtil.otpauthUri(issuer, account, secret);
        return new TotpSetup(secret, uri);
    }

    @Transactional
    public void enableTotp(final long userId, final String code) {
        final Instant now = Instant.now(clock);
        final String secret = userSecurityRepository.getTotpSecret(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.BAD_REQUEST, "totp_not_configured"));

        if (!TotpUtil.verifyCode(secret, code, now)) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "invalid_totp_code");
        }

        userSecurityRepository.setMfa(userId, true, METHOD_TOTP);
    }

    @Transactional
    public void disableTotp(final long userId) {
        userSecurityRepository.setMfa(userId, false, null);
        userSecurityRepository.clearTotpSecret(userId);
    }
}