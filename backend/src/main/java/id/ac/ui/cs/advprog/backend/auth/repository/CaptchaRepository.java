package id.ac.ui.cs.advprog.backend.auth.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CaptchaRepository {

    private final JdbcTemplate jdbcTemplate;

    public CaptchaRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void create(
            final UUID id,
            final String challengeType,
            final String promptText,
            final String answerHash,
            final Instant expiresAt,
            final Instant createdAt
    ) {
        jdbcTemplate.update(
            """
                INSERT INTO app_captcha_challenges(id, challenge_type, prompt_text, answer_hash, expires_at, used_at, created_at)
                VALUES (?, ?, ?, ?, ?, NULL, ?)
                """,
                id,
                challengeType,
                promptText,
                answerHash,
                odt(expiresAt),
                odt(createdAt)
        );
    }

    public Optional<CaptchaRow> findById(final UUID id) {
        final List<CaptchaRow> rows = jdbcTemplate.query(
                """
                SELECT id, challenge_type, prompt_text, answer_hash, expires_at, used_at, created_at
                FROM app_captcha_challenges
                WHERE id = ?
                """,
                (rs, n) -> new CaptchaRow(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("challenge_type"),
                        rs.getString("prompt_text"),
                        rs.getString("answer_hash"),
                        rs.getObject("expires_at", OffsetDateTime.class),
                        rs.getObject("used_at", OffsetDateTime.class),
                        rs.getObject("created_at", OffsetDateTime.class)
                ),
                id
        );
        return rows.stream().findFirst();
    }

    public void markUsed(final UUID id, final Instant usedAt) {
        jdbcTemplate.update("UPDATE app_captcha_challenges SET used_at = ? WHERE id = ?", odt(usedAt), id);
    }

    public int deleteExpiredBefore(final Instant cutoff) {
        return jdbcTemplate.update("DELETE FROM app_captcha_challenges WHERE expires_at < ?", odt(cutoff));
    }

    public int deleteUsedBefore(final Instant cutoff) {
        return jdbcTemplate.update("DELETE FROM app_captcha_challenges WHERE used_at IS NOT NULL AND used_at < ?", odt(cutoff));
    }

    private static OffsetDateTime odt(final Instant value) {
        return OffsetDateTime.ofInstant(value, ZoneOffset.UTC);
    }

    public record CaptchaRow(
            UUID id,
            String challengeType,
            String promptText,
            String answerHash,
            OffsetDateTime expiresAt,
            OffsetDateTime usedAt,
            OffsetDateTime createdAt
    ) {}
}
