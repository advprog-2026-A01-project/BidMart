package id.ac.ui.cs.advprog.backend.admin.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.backend.auth.repository.OutboxRepository;
import id.ac.ui.cs.advprog.backend.auth.repository.SessionRepository;
import id.ac.ui.cs.advprog.backend.auth.repository.UserAdminRepository;
import id.ac.ui.cs.advprog.backend.auth.repository.UserAuthRepository;
import id.ac.ui.cs.advprog.backend.auth.util.ClockHolder;
import id.ac.ui.cs.advprog.backend.rbac.repository.RbacRepository;
import id.ac.ui.cs.advprog.backend.security.RequiresPermission;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import id.ac.ui.cs.advprog.backend.auth.model.AuthException;
import id.ac.ui.cs.advprog.backend.auth.model.AuthPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private static final String ERROR_KEY = "error";
    private static final String ROLE_ADMIN = "ADMIN";

    private final UserAdminRepository userAdminRepository;
    private final UserAuthRepository userAuthRepository;
    private final SessionRepository sessionRepository;
    private final RbacRepository rbacRepository;
    private final ClockHolder clockHolder;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public AdminUserController(
            final UserAdminRepository userAdminRepository,
            final UserAuthRepository userAuthRepository,
            final SessionRepository sessionRepository,
            final RbacRepository rbacRepository,
            final ClockHolder clockHolder,
            final OutboxRepository outboxRepository,
            final ObjectMapper objectMapper
    ) {
        this.userAdminRepository = userAdminRepository;
        this.userAuthRepository = userAuthRepository;
        this.sessionRepository = sessionRepository;
        this.rbacRepository = rbacRepository;
        this.clockHolder = clockHolder;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    @RequiresPermission("users:read")
    public ResponseEntity<?> listUsers() {
        return ResponseEntity.ok(userAdminRepository.listUsers());
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

        // update role
        userAuthRepository.updateRoleName(id, roleName);

        // IMPORTANT: revoke all sessions so user must re-login and get fresh role/permissions
        sessionRepository.revokeAllByUserId(id, Instant.now(clockHolder.clock()));

        // outbox event
        final String payload = objectMapper.writeValueAsString(Map.of("userId", id, "newRole", roleName));
        outboxRepository.append(
                new OutboxRepository.OutboxEvent("UserRoleChanged", "User", String.valueOf(id), payload),
                Instant.now(clockHolder.clock())
        );

        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/{id}/disable")
    @RequiresPermission("users:write")
    @Transactional
    public ResponseEntity<?> disable(@PathVariable("id") final long id, @RequestBody final DisableUpdate body) throws Exception {
        final boolean disabled = body.disabled();
        userAuthRepository.setDisabled(id, disabled);

        if (disabled) {
            sessionRepository.revokeAllByUserId(id, Instant.now(clockHolder.clock()));
        }

        final String payload = objectMapper.writeValueAsString(Map.of("userId", id, "disabled", disabled));
        outboxRepository.append(
                new OutboxRepository.OutboxEvent("UserDisabledChanged", "User", String.valueOf(id), payload),
                Instant.now(clockHolder.clock())
        );

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

    @DeleteMapping("/{id}")
    @RequiresPermission("users:write")
    @Transactional
    public ResponseEntity<?> deleteUser(@PathVariable("id") final long id, final Authentication authentication) throws Exception {
        final AuthPrincipal principal = requirePrincipal(authentication);

        if (principal.userId() == id) {
            return ResponseEntity.badRequest().body(err("cannot_delete_self"));
        }

        final var targetOpt = userAdminRepository.findUserById(id);
        if (targetOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err("user_not_found"));
        }

        final var target = targetOpt.get();
        if (ROLE_ADMIN.equalsIgnoreCase(target.role())) {
            return ResponseEntity.badRequest().body(err("cannot_delete_admin"));
        }

        final int deleted = userAdminRepository.deleteUserById(id);
        if (deleted == 0) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err("user_not_found"));
        }

        final String payload = objectMapper.writeValueAsString(Map.of(
                "userId", id,
                "username", target.username(),
                "role", target.role()
        ));

        outboxRepository.append(
                new OutboxRepository.OutboxEvent("UserDeleted", "User", String.valueOf(id), payload),
                Instant.now(clockHolder.clock())
        );

        return ResponseEntity.ok(Map.of("ok", true));
    }

    private static AuthPrincipal requirePrincipal(final Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal p)) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "unauthorized");
        }
        return p;
    }
}