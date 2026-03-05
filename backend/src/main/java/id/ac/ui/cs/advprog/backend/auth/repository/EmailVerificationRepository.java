package id.ac.ui.cs.advprog.backend.auth.repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EmailVerificationRepository {

    private final JdbcTemplate jdbcTemplate;

    public EmailVerificationRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static OffsetDateTime odt(final Instant t) {
        return OffsetDateTime.ofInstant(t, ZoneOffset.UTC);
    }

    public UUID createToken(final long userId, final Instant now, final Instant expiresAt) {
        final UUID token = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO app_email_verifications(token, user_id, expires_at) VALUES (?, ?, ?)",
                token, userId, odt(expiresAt)
        );
        return token;
    }

    public Optional<Long> consumeIfValid(final UUID token, final Instant now) {
        final var rows = jdbcTemplate.query(
                """
                SELECT user_id
                FROM app_email_verifications
                WHERE token = ?
                  AND used_at IS NULL
                  AND expires_at > ?
                """,
                (rs, n) -> rs.getLong("user_id"),
                token, odt(now)
        );
        if (rows.isEmpty()) return Optional.empty();

        jdbcTemplate.update("UPDATE app_email_verifications SET used_at = ? WHERE token = ?", odt(now), token);
        return Optional.of(rows.get(0));
    }
}