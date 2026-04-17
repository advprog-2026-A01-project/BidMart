package id.ac.ui.cs.advprog.backend.auth.repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserSecurityRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserSecurityRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setMfa(final long userId, final boolean enabled, final String method) {
        jdbcTemplate.update(
                "UPDATE app_users SET mfa_enabled = ?, mfa_method = ? WHERE id = ?",
                enabled,
                method,
                userId
        );
    }

    public void setTotpSecret(final long userId, final String secret) {
        jdbcTemplate.update("UPDATE app_users SET totp_secret = ? WHERE id = ?", secret, userId);
    }

    public void clearTotpSecret(final long userId) {
        jdbcTemplate.update("UPDATE app_users SET totp_secret = NULL WHERE id = ?", userId);
    }

    public Optional<String> getTotpSecret(final long userId) {
        final List<String> rows = jdbcTemplate.query(
                "SELECT totp_secret FROM app_users WHERE id = ?",
                (rs, n) -> rs.getString("totp_secret"),
                userId
        );
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        final String value = rows.get(0);
        return (value == null || value.isBlank()) ? Optional.empty() : Optional.of(value);
    }

    public void updatePasswordHash(final long userId, final String passwordHash) {
        jdbcTemplate.update("UPDATE app_users SET password_hash = ? WHERE id = ?", passwordHash, userId);
    }

    public void updatePersonalKey(final long userId, final String personalKeyHash, final Instant rotatedAt) {
        jdbcTemplate.update(
                "UPDATE app_users SET personal_key_hash = ?, personal_key_rotated_at = ? WHERE id = ?",
                personalKeyHash,
                OffsetDateTime.ofInstant(rotatedAt, ZoneOffset.UTC),
                userId
        );
    }
}