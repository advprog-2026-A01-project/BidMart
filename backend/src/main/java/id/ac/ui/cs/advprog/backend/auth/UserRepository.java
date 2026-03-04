package id.ac.ui.cs.advprog.backend.auth;

import java.sql.PreparedStatement;
import java.sql.Statement;
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

    public Optional<UserRow> findByUsername(String username) {
        var rows = jdbcTemplate.query(
            "SELECT id, username, password_hash, role, is_disabled FROM app_users WHERE username = ?",
            (rs, n) -> new UserRow(
                    rs.getLong("id"),
                    rs.getString("username"),
                    Role.fromDb(rs.getString("role")),
                    rs.getString("password_hash"),
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
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setString(3, role.name());
            return ps;
        }, keyHolder);

        final var key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("failed_to_insert_user");
        }

        return key.longValue();
    }

    public void setDisabled(final long userId, final boolean disabled) {
        jdbcTemplate.update("UPDATE app_users SET is_disabled = ? WHERE id = ?", disabled, userId);
    }

    public record UserRow(long id, String username, String passwordHash, Role role, boolean disabled) {

    }
}
