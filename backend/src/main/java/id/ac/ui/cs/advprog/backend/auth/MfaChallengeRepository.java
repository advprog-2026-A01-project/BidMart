package id.ac.ui.cs.advprog.backend.auth;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MfaChallengeRepository {

    private final JdbcTemplate jdbcTemplate;

    public MfaChallengeRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static OffsetDateTime odt(final Instant t) {
        return OffsetDateTime.ofInstant(t, ZoneOffset.UTC);
    }

    public UUID createEmailChallenge(final long userId, final String codeHash, final Instant expiresAt) {
        final UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO app_mfa_challenges(id, user_id, method, code_hash, expires_at)
                VALUES (?, ?, 'EMAIL', ?, ?)
                """,
                id, userId, codeHash, odt(expiresAt)
        );
        return id;
    }

    public Optional<Row> findById(final UUID id) {
        final var rows = jdbcTemplate.query(
                """
                SELECT id, user_id, method, code_hash, expires_at, used_at, attempts
                FROM app_mfa_challenges
                WHERE id = ?
                """,
                (rs, n) -> new Row(
                        (UUID) rs.getObject("id"),
                        rs.getLong("user_id"),
                        rs.getString("method"),
                        rs.getString("code_hash"),
                        rs.getObject("expires_at", OffsetDateTime.class),
                        rs.getObject("used_at", OffsetDateTime.class),
                        rs.getInt("attempts")
                ),
                id
        );
        return rows.stream().findFirst();
    }

    public void markUsed(final UUID id, final Instant now) {
        jdbcTemplate.update("UPDATE app_mfa_challenges SET used_at = ? WHERE id = ?", odt(now), id);
    }

    public void incrementAttempts(final UUID id) {
        jdbcTemplate.update("UPDATE app_mfa_challenges SET attempts = attempts + 1 WHERE id = ?", id);
    }

    public record Row(
            UUID id,
            long userId,
            String method,
            String codeHash,
            OffsetDateTime expiresAt,
            OffsetDateTime usedAt,
            int attempts
    ) {}
}