package id.ac.ui.cs.advprog.backend.auth.repository;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserAdminRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserAdminRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<UserSummary> listUsers() {
        return jdbcTemplate.query(
                "SELECT id, username, role, is_disabled, created_at FROM app_users ORDER BY id",
                (rs, n) -> new UserSummary(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("role"),
                        rs.getBoolean("is_disabled"),
                        rs.getObject("created_at", java.time.OffsetDateTime.class)
                )
        );
    }

    public record UserSummary(
            long id,
            String username,
            String role,
            boolean disabled,
            java.time.OffsetDateTime createdAt
    ) {}
}