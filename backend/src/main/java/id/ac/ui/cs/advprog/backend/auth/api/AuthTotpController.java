package id.ac.ui.cs.advprog.backend.auth.api;

import id.ac.ui.cs.advprog.backend.auth.model.AuthException;
import id.ac.ui.cs.advprog.backend.auth.model.AuthPrincipal;
import id.ac.ui.cs.advprog.backend.auth.service.AuthMfaManagementService;
import id.ac.ui.cs.advprog.backend.security.RequiresPermission;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/2fa/totp")
public class AuthTotpController {

    private static final String PERM_MFA_MANAGE = "mfa:manage";

    private final AuthMfaManagementService mfaManagementService;

    public AuthTotpController(final AuthMfaManagementService mfaManagementService) {
        this.mfaManagementService = mfaManagementService;
    }

    @PostMapping("/setup")
    @RequiresPermission(PERM_MFA_MANAGE)
    public ResponseEntity<?> setup(final Authentication authentication) {
        final AuthPrincipal p = requirePrincipal(authentication);
        final var setup = mfaManagementService.setupTotp(p.userId(), p.username());
        return ResponseEntity.ok(new TotpSetupResponse(true, setup.secret(), setup.otpauthUrl()));
    }

    @PostMapping("/enable")
    @RequiresPermission(PERM_MFA_MANAGE)
    public ResponseEntity<?> enable(final Authentication authentication, @RequestBody final TotpEnableRequest body) {
        final AuthPrincipal p = requirePrincipal(authentication);
        mfaManagementService.enableTotp(p.userId(), body == null ? null : body.code());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/disable")
    @RequiresPermission(PERM_MFA_MANAGE)
    public ResponseEntity<?> disable(final Authentication authentication) {
        final AuthPrincipal p = requirePrincipal(authentication);
        mfaManagementService.disableTotp(p.userId());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    private static AuthPrincipal requirePrincipal(final Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal p)) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "unauthorized");
        }
        return p;
    }

    public record TotpSetupResponse(boolean ok, String secret, String otpauthUrl) {}
    public record TotpEnableRequest(String code) {}
}