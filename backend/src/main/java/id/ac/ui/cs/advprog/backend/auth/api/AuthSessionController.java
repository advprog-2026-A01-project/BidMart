package id.ac.ui.cs.advprog.backend.auth.api;

import id.ac.ui.cs.advprog.backend.auth.model.AuthException;
import id.ac.ui.cs.advprog.backend.auth.model.AuthPrincipal;
import id.ac.ui.cs.advprog.backend.auth.repository.SessionRepository;
import id.ac.ui.cs.advprog.backend.auth.service.AuthTokenService;
import id.ac.ui.cs.advprog.backend.security.RequiresPermission;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthSessionController {

    private static final String ERROR_KEY = "error";
    private static final String PERM_SESSION_ME = "session:me";
    private static final String PERM_SESSION_LIST = "session:list";
    private static final String PERM_SESSION_REVOKE = "session:revoke";
    private static final String PERM_SESSION_LOGOUT = "session:logout";

    private final SessionRepository sessionRepository;
    private final AuthTokenService tokenService;

    public AuthSessionController(final SessionRepository sessionRepository, final AuthTokenService tokenService) {
        this.sessionRepository = sessionRepository;
        this.tokenService = tokenService;
    }

    @GetMapping("/me")
    @RequiresPermission(PERM_SESSION_ME)
    public ResponseEntity<?> me(final Authentication authentication) {
        final AuthPrincipal p = requirePrincipal(authentication);
        return ResponseEntity.ok(Map.of("username", p.username(), "role", p.role()));
    }

    @PostMapping("/logout")
    @RequiresPermission(PERM_SESSION_LOGOUT)
    public ResponseEntity<?> logout(final Authentication authentication) {
        if (authentication == null || authentication.getDetails() == null) {
            return ResponseEntity.status(401).body(Map.of(ERROR_KEY, "unauthorized"));
        }
        tokenService.logout(String.valueOf(authentication.getDetails()));
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/sessions")
    @RequiresPermission(PERM_SESSION_LIST)
    public List<SessionRepository.SessionRow> sessions(final Authentication authentication) {
        final AuthPrincipal p = requirePrincipal(authentication);
        return sessionRepository.listSessions(p.userId());
    }

    @PostMapping("/sessions/{token}/revoke")
    @RequiresPermission(PERM_SESSION_REVOKE)
    public ResponseEntity<?> revoke(@PathVariable("token") final String token, final Authentication authentication) {
        final AuthPrincipal p = requirePrincipal(authentication);

        final UUID t;
        try {
            t = UUID.fromString(token.trim());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "invalid_token"));
        }

        final int updated = sessionRepository.revokeByTokenAndUserId(t, p.userId(), Instant.now());
        if (updated == 0) return ResponseEntity.status(404).body(Map.of(ERROR_KEY, "session_not_found"));
        return ResponseEntity.ok(Map.of("ok", true));
    }

    private static AuthPrincipal requirePrincipal(final Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal p)) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "unauthorized");
        }
        return p;
    }
}