package id.ac.ui.cs.advprog.backend.auth;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RbacRepository {

    private final JdbcTemplate jdbcTemplate;

    public RbacRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean roleExists(final String roleName) {
        final Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app_roles WHERE name = ?",
                Integer.class, roleName
        );
        return n != null && n > 0;
    }

    public void createRole(final String roleName) {
        jdbcTemplate.update("INSERT INTO app_roles(name) VALUES (?) ON CONFLICT DO NOTHING", roleName);
    }

    public void createPermission(final String key, final String description) {
        jdbcTemplate.update(
                "INSERT INTO app_permissions(perm_key, description) VALUES (?, ?) ON CONFLICT DO NOTHING",
                key, description
        );
    }

    public List<String> listRoles() {
        return jdbcTemplate.query("SELECT name FROM app_roles ORDER BY name", (rs, n) -> rs.getString("name"));
    }

    public List<PermissionRow> listPermissions() {
        return jdbcTemplate.query(
                "SELECT perm_key, description FROM app_permissions ORDER BY perm_key",
                (rs, n) -> new PermissionRow(rs.getString("perm_key"), rs.getString("description"))
        );
    }

    public List<String> listPermissionsForRole(final String roleName) {
        return jdbcTemplate.query(
                """
                SELECT perm_key
                FROM app_role_permissions
                WHERE role_name = ?
                ORDER BY perm_key
                """,
                (rs, n) -> rs.getString("perm_key"),
                roleName
        );
    }

    public void setRolePermissions(final String roleName, final List<String> permKeys) {
        jdbcTemplate.update("DELETE FROM app_role_permissions WHERE role_name = ?", roleName);
        for (String k : permKeys) {
            jdbcTemplate.update("INSERT INTO app_role_permissions(role_name, perm_key) VALUES (?, ?)", roleName, k);
        }
    }

    public record PermissionRow(String key, String description) {}
}