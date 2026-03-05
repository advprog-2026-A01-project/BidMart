package id.ac.ui.cs.advprog.backend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.backend.security.RequiresPermission;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private static final String ERROR_KEY = "error";

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final RbacRepository rbacRepository;
    private final ClockHolder clockHolder;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public AdminUserController(
            final UserRepository userRepository,
            final SessionRepository sessionRepository,
            final RbacRepository rbacRepository,
            final ClockHolder clockHolder,
            final OutboxRepository outboxRepository,
            final ObjectMapper objectMapper
    ) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.rbacRepository = rbacRepository;
        this.clockHolder = clockHolder;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    @RequiresPermission("users:read")
    public ResponseEntity<?> listUsers() {
        return ResponseEntity.ok(userRepository.listUsers());
    }

    @PostMapping("/{id}/role")
    @RequiresPermission("users:write")
    @Transactional
    public ResponseEntity<?> setRole(@PathVariable("id") final long id, @RequestBody final RoleUpdate body) throws Exception {
        final String roleName = normalizeRoleName(body.role());
        if (roleName == null) return ResponseEntity.badRequest().body(err("invalid_role"));

        if (!rbacRepository.roleExists(roleName)) {
            return ResponseEntity.badRequest().body(err("role_not_found"));
        }

        userRepository.updateRoleName(id, roleName);

        final String payload = objectMapper.writeValueAsString(Map.of(
                "userId", id,
                "newRole", roleName
        ));
        outboxRepository.append("UserRoleChanged", "User", String.valueOf(id), payload, Instant.now(clockHolder.clock()));

        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/{id}/disable")
    @RequiresPermission("users:write")
    @Transactional
    public ResponseEntity<?> disable(@PathVariable("id") final long id, @RequestBody final DisableUpdate body) throws Exception {
        final boolean disabled = body.disabled();
        userRepository.setDisabled(id, disabled);

        if (disabled) {
            sessionRepository.revokeAllByUserId(id, Instant.now(clockHolder.clock()));
        }

        final String payload = objectMapper.writeValueAsString(Map.of(
                "userId", id,
                "disabled", disabled
        ));
        outboxRepository.append("UserDisabledChanged", "User", String.valueOf(id), payload, Instant.now(clockHolder.clock()));

        return ResponseEntity.ok(Map.of("ok", true));
    }

    private static Map<String, String> err(final String code) {
        return Map.of(ERROR_KEY, code);
    }

    private static String normalizeRoleName(final String raw) {
        if (raw == null) return null;
        final String s = raw.trim();
        if (s.isBlank()) return null;
        if (!s.matches("[A-Za-z0-9_\\-]{3,64}")) return null;
        return s.toUpperCase(Locale.ROOT);
    }

    public record RoleUpdate(String role) {}
    public record DisableUpdate(boolean disabled) {}
}