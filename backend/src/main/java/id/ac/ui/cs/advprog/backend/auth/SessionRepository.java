package id.ac.ui.cs.advprog.backend.auth;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/*
Tanggung jawab: operasi DB untuk session/token.

- create token pair
- validate access token
- rotate refresh token
- revoke/logout
- limit concurrent sessions
- list sessions
*/
@Repository
public class SessionRepository {

    private final JdbcTemplate jdbcTemplate;

    public SessionRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public TokenPair create(
            final long userId,
            final Instant now,
            final Instant accessExpiresAt,
            final Instant refreshExpiresAt,
            final String userAgent,
            final String ip
    ) {
        final UUID accessToken = UUID.randomUUID();
        final UUID refreshToken = UUID.randomUUID();

        jdbcTemplate.update(
        """
            INSERT INTO app_sessions(token, refresh_token, user_id, created_at, last_seen_at, expires_at, refresh_expires_at, user_agent, ip)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            accessToken,
            refreshToken,
            userId,
            odt(now),
            odt(now),
            odt(accessExpiresAt),
            odt(refreshExpiresAt),
            userAgent,
            ip
        );

        return new TokenPair(accessToken, refreshToken, accessExpiresAt, refreshExpiresAt);
    }

    public Optional<AuthSession> findActiveByAccessToken(final String tokenString, final Instant now) {
        final UUID token;
        try {
            token = UUID.fromString(tokenString);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }

        final var rows = jdbcTemplate.query(
        """
            SELECT u.id AS user_id, u.username, u.role
            FROM app_sessions s
            JOIN app_users u ON u.id = s.user_id
            WHERE s.token = ?
              AND (s.revoked_at IS NULL)
              AND (s.expires_at IS NULL OR s.expires_at > ?)
              AND (u.is_disabled = FALSE)
            """,
                (rs, n) -> new AuthSession(
                        rs.getLong("user_id"),
                        rs.getString("username"),
                        Role.fromDb(rs.getString("role"))
                ),
                token,
                odt(now)
        );
        if (rows.isEmpty()) return Optional.empty();

        // best-effort last-seen update
        jdbcTemplate.update("UPDATE app_sessions SET last_seen_at = ? WHERE token = ?", odt(now), token);

        return Optional.of(rows.get(0));
    }

    public Optional<TokenPair> rotateByRefreshToken(
            final String refreshTokenString,
            final Instant now,
            final Instant newAccessExpiresAt,
            final Instant newRefreshExpiresAt,
            final String userAgent,
            final String ip
    ) {
        final UUID refreshToken;
        try {
            refreshToken = UUID.fromString(refreshTokenString);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }

        final var userIds = jdbcTemplate.query(
                """
                SELECT user_id
                FROM app_sessions
                WHERE refresh_token = ?
                  AND revoked_at IS NULL
                  AND (refresh_expires_at IS NULL OR refresh_expires_at > ?)
                """,
                (rs, n) -> rs.getLong("user_id"),
                refreshToken,
                odt(now)
        );
        if (userIds.isEmpty()) return Optional.empty();

        final long userId = userIds.get(0);

        // revoke old session row (hard delete to keep schema simple)
        jdbcTemplate.update("DELETE FROM app_sessions WHERE refresh_token = ?", refreshToken);

        return Optional.of(create(userId, now, newAccessExpiresAt, newRefreshExpiresAt, userAgent, ip));
    }

    public void revokeByAccessToken(final String tokenString, final Instant now) {
        final UUID token;
        try {
            token = UUID.fromString(tokenString);
        } catch (IllegalArgumentException e) {
            return;
        }
        jdbcTemplate.update("UPDATE app_sessions SET revoked_at = ? WHERE token = ?", odt(now), token);
    }

    // Fungsi helper untuk login pakai fungsi countActiveSessions()
    private static OffsetDateTime odt(final Instant t) {
        return OffsetDateTime.ofInstant(t, ZoneOffset.UTC);
    }

    public int countActiveSessions(final long userId, final Instant now) {
        final Integer n = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM app_sessions
                WHERE user_id = ?
                  AND revoked_at IS NULL
                  AND (refresh_expires_at IS NULL OR refresh_expires_at > ?)
                """,
                Integer.class,
                userId,
                odt(now) // Ini yang diubah
        );
        return n == null ? 0 : n;
    }

    public int revokeOldestSessions(final long userId, final int limit, final Instant now) {
        if (limit <= 0) return 0;

        final List<UUID> tokens = jdbcTemplate.query(
                """
                SELECT token
                FROM app_sessions
                WHERE user_id = ?
                  AND revoked_at IS NULL
                ORDER BY created_at ASC
                LIMIT ?
                """,
                (rs, n) -> (UUID) rs.getObject("token"),
                userId,
                limit
        );

        int revoked = 0;
        for (UUID t : tokens) {
            revoked += jdbcTemplate.update("UPDATE app_sessions SET revoked_at = ? WHERE token = ?", odt(now), t);
        }
        return revoked;
    }

    public List<SessionRow> listSessions(final long userId) {
        return jdbcTemplate.query(
                """
                SELECT token, created_at, last_seen_at, expires_at, revoked_at, user_agent, ip
                FROM app_sessions
                WHERE user_id = ?
                ORDER BY created_at DESC
                """,
                (rs, n) -> new SessionRow(
                        (UUID) rs.getObject("token"),
                        rs.getObject("created_at", java.time.OffsetDateTime.class),
                        rs.getObject("last_seen_at", java.time.OffsetDateTime.class),
                        rs.getObject("expires_at", java.time.OffsetDateTime.class),
                        rs.getObject("revoked_at", java.time.OffsetDateTime.class),
                        rs.getString("user_agent"),
                        rs.getString("ip")
                ),
                userId
        );
    }

    public record AuthSession(long userId, String username, Role role) {}

    public record TokenPair(UUID accessToken, UUID refreshToken, Instant accessExpiresAt, Instant refreshExpiresAt) {}

    public record SessionRow(
            UUID token,
            java.time.OffsetDateTime createdAt,
            java.time.OffsetDateTime lastSeenAt,
            java.time.OffsetDateTime expiresAt,
            java.time.OffsetDateTime revokedAt,
            String userAgent,
            String ip
    ) {}
}
