package id.ac.ui.cs.advprog.backend.auth;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private static final String ERROR_KEY = "error";

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final ClockHolder clockHolder;

    public AdminUserController(
            final UserRepository userRepository,
            final SessionRepository sessionRepository,
            final ClockHolder clockHolder
    ) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.clockHolder = clockHolder;
    }

    @GetMapping
    public ResponseEntity<?> listUsers() {
        return ResponseEntity.ok(userRepository.listUsers());
    }

    @PostMapping("/{id}/role")
    public ResponseEntity<?> setRole(@PathVariable("id") final long id, @RequestBody final RoleUpdate body) {
        final Role role = parseRole(body.role());
        if (role == null) {
            return ResponseEntity.badRequest().body(err("invalid_role"));
        }

        userRepository.updateRole(id, role);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/{id}/disable")
    public ResponseEntity<?> disable(@PathVariable("id") final long id, @RequestBody final DisableUpdate body) {
        final boolean disabled = body.disabled();
        userRepository.setDisabled(id, disabled);

        // If disabled, invalidate all sessions
        if (disabled) {
            sessionRepository.revokeAllByUserId(id, Instant.now(clockHolder.clock()));
        }

        return ResponseEntity.ok(Map.of("ok", true));
    }

    private static Map<String, String> err(final String code) {
        return Map.of(ERROR_KEY, code);
    }

    private static Role parseRole(final String role) {
        if (role == null) return null;
        final String r = role.trim().toUpperCase(Locale.ROOT);
        return switch (r) {
            case "ADMIN" -> Role.ADMIN;
            case "SELLER" -> Role.SELLER;
            case "BUYER" -> Role.BUYER;
            default -> null;
        };
    }

    public record RoleUpdate(String role) {}
    public record DisableUpdate(boolean disabled) {}
}