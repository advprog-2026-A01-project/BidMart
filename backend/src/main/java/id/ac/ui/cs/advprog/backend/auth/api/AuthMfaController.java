package id.ac.ui.cs.advprog.backend.auth.api;

import id.ac.ui.cs.advprog.backend.auth.model.AuthException;
import id.ac.ui.cs.advprog.backend.auth.model.AuthPrincipal;
import id.ac.ui.cs.advprog.backend.auth.service.AuthLoginService;
import id.ac.ui.cs.advprog.backend.auth.service.AuthRegistrationService;
import id.ac.ui.cs.advprog.backend.security.RequiresPermission;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/2fa")
public class AuthMfaController {

    private static final String PERM_MFA_MANAGE = "mfa:manage";

    private final AuthLoginService loginService;
    private final AuthRegistrationService registrationService;

    public AuthMfaController(final AuthLoginService loginService, final AuthRegistrationService registrationService) {
        this.loginService = loginService;
        this.registrationService = registrationService;
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyMfa(@RequestBody final MfaVerifyRequest body, final HttpServletRequest request) {
        final var meta = new AuthLoginService.ClientMeta(request.getHeader("User-Agent"), request.getRemoteAddr());
        final var tokens = loginService.verifyMfa(body.challengeId(), body.code(), meta);
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/enable-email")
    @RequiresPermission(PERM_MFA_MANAGE)
    public ResponseEntity<?> enableEmail(final Authentication authentication) {
        final AuthPrincipal p = requirePrincipal(authentication);
        loginService.enableEmailMfa(p.userId());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/disable")
    @RequiresPermission(PERM_MFA_MANAGE)
    public ResponseEntity<?> disable(final Authentication authentication) {
        final AuthPrincipal p = requirePrincipal(authentication);
        loginService.disableMfa(p.userId());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/private-key/rotate")
    @RequiresPermission(PERM_MFA_MANAGE)
    public ResponseEntity<?> rotatePrivateKey(final Authentication authentication) {
        final AuthPrincipal p = requirePrincipal(authentication);
        final var result = registrationService.rotatePersonalKey(p.userId());
        return ResponseEntity.ok(new PrivateKeyRotateResponse(
                result.ok(),
                result.privateKey(),
                result.downloadFilename(),
                result.downloadContent(),
                result.issuedAt()
        ));
    }

    private static AuthPrincipal requirePrincipal(final Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal p)) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "unauthorized");
        }
        return p;
    }

    public record MfaVerifyRequest(String challengeId, String code) {}

    public record PrivateKeyRotateResponse(
            boolean ok,
            String privateKey,
            String downloadFilename,
            String downloadContent,
            String issuedAt
    ) {}
}