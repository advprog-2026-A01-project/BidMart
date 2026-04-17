package id.ac.ui.cs.advprog.backend.auth.repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
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
        return createToken(userId, now, expiresAt, null);
    }

    public UUID createToken(final long userId, final Instant now, final Instant expiresAt, final String demoCode) {
        final UUID token = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO app_email_verifications(token, user_id, expires_at, demo_code) VALUES (?, ?, ?, ?)",
                token, userId, odt(expiresAt), normalizeDemoCode(demoCode)
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

    public Optional<Long> consumeByUsernameAndCodeIfValid(final String username, final String code, final Instant now) {
        final String safeUsername = (username == null) ? "" : username.trim().toLowerCase(Locale.ROOT);
        final String safeCode = normalizeDemoCode(code);
        if (safeUsername.isBlank() || safeCode == null) {
            return Optional.empty();
        }

        final var rows = jdbcTemplate.query(
                """
                SELECT ev.token, ev.user_id
                FROM app_email_verifications ev
                JOIN app_users u ON u.id = ev.user_id
                WHERE u.username = ?
                  AND ev.demo_code = ?
                  AND ev.used_at IS NULL
                  AND ev.expires_at > ?
                ORDER BY ev.expires_at DESC
                LIMIT 1
                """,
                (rs, n) -> new ConsumeTarget((UUID) rs.getObject("token"), rs.getLong("user_id")),
                safeUsername,
                safeCode,
                odt(now)
        );
        if (rows.isEmpty()) return Optional.empty();

        final ConsumeTarget target = rows.get(0);
        jdbcTemplate.update("UPDATE app_email_verifications SET used_at = ? WHERE token = ?", odt(now), target.token());
        return Optional.of(target.userId());
    }

    private static String normalizeDemoCode(final String raw) {
        if (raw == null || raw.isBlank()) return null;
        return raw.trim();
    }

    private record ConsumeTarget(UUID token, long userId) {}
}