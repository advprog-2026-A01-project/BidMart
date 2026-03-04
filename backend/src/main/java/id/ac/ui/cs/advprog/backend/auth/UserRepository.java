package id.ac.ui.cs.advprog.backend.auth;

import java.sql.PreparedStatement;
import java.sql.Statement;
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
                    new String[] { "id" }
            );
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setString(3, role.name());
            return ps;
        }, keyHolder);

        // 1) Prefer ambil dari map keys (aman kalau driver ngembaliin banyak kolom)
        final Map<String, Object> keys = keyHolder.getKeys();
        if (keys != null) {
            final Object idObj = keys.get("id");
            if (idObj instanceof Number n) {
                return n.longValue();
            }
        }
        // 2) Fallback: single key (kalau memang cuman 1)
        final Number key = keyHolder.getKey();
        if (key != null) {
            return key.longValue();
        }

        throw new IllegalStateException("failed_to_insert_user");
    }

    public void setDisabled(final long userId, final boolean disabled) {
        jdbcTemplate.update("UPDATE app_users SET is_disabled = ? WHERE id = ?", disabled, userId);
    }

    public record UserRow(long id, String username, String passwordHash, Role role, boolean disabled) {}
}