package id.ac.ui.cs.advprog.backend.auth;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/*
Tanggung jawab: operasi DB untuk user.
- findByUsername
- insert
- setDisabled (fondasi admin)
*/

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UserRow> findByUsername(final String username) {
        final var rows = jdbcTemplate.query(
                "SELECT id, username, password_hash, role, is_disabled FROM app_users WHERE username = ?",
                (rs, n) -> new UserRow(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        Role.fromDb(rs.getString("role")),
                        rs.getBoolean("is_disabled")
                ),
                username
        );
        return rows.stream().findFirst();
    }

    public long insert(final String username, final String passwordHash, final Role role) {
        final KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(con -> {
            final PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO app_users(username, password_hash, role) VALUES (?, ?, ?)",
                    new String[]{"id"}
            );
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setString(3, role.name());
            return ps;
        }, keyHolder);

        final Long id = extractGeneratedId(keyHolder);
        if (id != null) return id;

        throw new IllegalStateException("failed_to_insert_user");
    }

    private static Long extractGeneratedId(final KeyHolder keyHolder) {
        final List<Map<String, Object>> keyList = keyHolder.getKeyList();
        if (keyList.isEmpty()) return null;

        final Map<String, Object> row = keyList.get(0);

        final Object idObj = row.containsKey("id") ? row.get("id") : row.get("ID");
        final Long direct = toLong(idObj);
        if (direct != null) return direct;

        // fallback: first numeric value in the key map
        for (Object v : row.values()) {
            final Long n = toLong(v);
            if (n != null) return n;
        }

        return null;
    }

    private static Long toLong(final Object obj) {
        if (obj instanceof Number n) return n.longValue();
        return null;
    }

    public void setDisabled(final long userId, final boolean disabled) {
        jdbcTemplate.update("UPDATE app_users SET is_disabled = ? WHERE id = ?", disabled, userId);
    }

    public record UserRow(long id, String username, String passwordHash, Role role, boolean disabled) {}
}