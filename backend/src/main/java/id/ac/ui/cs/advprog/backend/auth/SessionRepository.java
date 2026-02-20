package id.ac.ui.cs.advprog.backend.auth;

import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SessionRepository {

    private final JdbcTemplate jdbcTemplate;

    public SessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UUID create(long userId) {
        UUID token = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO app_sessions(token, user_id) VALUES (?, ?)", token, userId);
        return token;
    }

    public Optional<String> findUsernameByToken(String tokenString) {
        UUID token;
        try {
            token = UUID.fromString(tokenString);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }

        var rows = jdbcTemplate.query(
            """
            SELECT u.username
            FROM app_sessions s
            JOIN app_users u ON u.id = s.user_id
            WHERE s.token = ?
            """,
            (rs, n) -> rs.getString("username"),
            token
        );

        return rows.stream().findFirst();
    }
}
