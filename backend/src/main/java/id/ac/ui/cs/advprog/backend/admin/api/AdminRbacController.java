package id.ac.ui.cs.advprog.backend.admin.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.backend.auth.util.ClockHolder;
import id.ac.ui.cs.advprog.backend.auth.repository.OutboxRepository;
import id.ac.ui.cs.advprog.backend.rbac.repository.RbacRepository;
import id.ac.ui.cs.advprog.backend.security.RequiresPermission;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/rbac")
public class AdminRbacController {

    private static final String ERROR_KEY = "error";
    private final RbacRepository rbacRepository;
    private final OutboxRepository outboxRepository;
    private final ClockHolder clockHolder;
    private final ObjectMapper objectMapper;

    public AdminRbacController(
            final RbacRepository rbacRepository,
            final OutboxRepository outboxRepository,
            final ClockHolder clockHolder,
            final ObjectMapper objectMapper
    ) {
        this.rbacRepository = rbacRepository;
        this.outboxRepository = outboxRepository;
        this.clockHolder = clockHolder;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/roles")
    @RequiresPermission("rbac:read")
    public List<String> listRoles() {
        return rbacRepository.listRoles();
    }

    @PostMapping("/roles")
    @RequiresPermission("rbac:write")
    public ResponseEntity<?> createRole(@RequestBody final CreateRole body) {
        final String name = normalizeRoleName(body.name());
        if (name == null) return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "invalid_role"));
        rbacRepository.createRole(name);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/permissions")
    @RequiresPermission("rbac:read")
    public List<RbacRepository.PermissionRow> listPermissions() {
        return rbacRepository.listPermissions();
    }

    @PostMapping("/permissions")
    @RequiresPermission("rbac:write")
    public ResponseEntity<?> createPermission(@RequestBody final CreatePermission body) {
        final String key = normalizePermKey(body.key());
        if (key == null) return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "invalid_permission"));
        rbacRepository.createPermission(key, body.description());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/roles/{role}/permissions")
    @RequiresPermission("rbac:read")
    public ResponseEntity<?> listRolePerms(@PathVariable("role") final String role) {
        return ResponseEntity.ok(rbacRepository.listPermissionsForRole(role));
    }

    @PutMapping("/roles/{role}/permissions")
    @RequiresPermission("rbac:write")
    @Transactional
    public ResponseEntity<?> setRolePerms(@PathVariable("role") final String role, @RequestBody final SetRolePerms body) throws Exception {
        if (!rbacRepository.roleExists(role)) return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "role_not_found"));
        final List<String> perms = body.permissions() == null ? List.of() : body.permissions();
        rbacRepository.setRolePermissions(role, perms);

        final String payload = objectMapper.writeValueAsString(Map.of(
                "role", role,
                "permissions", perms
        ));
        outboxRepository.append("RolePermissionsChanged", "Role", role, payload, Instant.now(clockHolder.clock()));

        return ResponseEntity.ok(Map.of("ok", true));
    }

    private static String normalizeRoleName(final String raw) {
        if (raw == null) return null;
        final String s = raw.trim();
        if (s.isBlank()) return null;
        if (!s.matches("[A-Za-z0-9_\\-]{3,64}")) return null;
        return s.toUpperCase(Locale.ROOT);
    }

    private static String normalizePermKey(final String raw) {
        if (raw == null) return null;
        final String s = raw.trim();
        if (s.isBlank()) return null;
        if (!s.matches("[a-z]+:[a-z_]+")) return null;
        return s;
    }

    public record CreateRole(String name) {}
    public record CreatePermission(String key, String description) {}
    public record SetRolePerms(List<String> permissions) {}
}