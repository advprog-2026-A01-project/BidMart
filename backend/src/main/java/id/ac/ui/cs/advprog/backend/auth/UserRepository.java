package id.ac.ui.cs.advprog.backend.auth;

import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UserRow> findByUsername(String username) {
        var rows = jdbcTemplate.query(
            "SELECT id, username, password_hash FROM app_users WHERE username = ?",
            (rs, n) -> new UserRow(rs.getLong("id"), rs.getString("username"), rs.getString("password_hash")),
            username
        );
        return rows.stream().findFirst();
    }

    public long insert(String username, String passwordHash) {
        return jdbcTemplate.queryForObject(
            "INSERT INTO app_users(username, password_hash) VALUES (?, ?) RETURNING id",
            Long.class,
            username,
            passwordHash
        );
    }

    public record UserRow(long id, String username, String passwordHash) {}
}
