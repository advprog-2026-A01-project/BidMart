package id.ac.ui.cs.advprog.backend.auth.repository;

import id.ac.ui.cs.advprog.backend.auth.model.Role;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class UserAuthRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserAuthRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UserRow> findByUsername(final String username) {
        final var rows = jdbcTemplate.query(
                """
                SELECT id, username, password_hash, role, is_disabled, email_verified, mfa_enabled, mfa_method, totp_secret
                FROM app_users
                WHERE username = ?
                """,
                (rs, n) -> new UserRow(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        Role.fromDb(rs.getString("role")),
                        rs.getBoolean("is_disabled"),
                        rs.getBoolean("email_verified"),
                        rs.getBoolean("mfa_enabled"),
                        rs.getString("mfa_method"),
                        rs.getString("totp_secret")
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
                    new String[] {"id"}
            );
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setString(3, role.name());
            return ps;
        }, keyHolder);

        final Long id = extractGeneratedId(keyHolder);
        if (id == null) throw new IllegalStateException("failed_to_insert_user");
        return id;
    }

    public void setDisabled(final long userId, final boolean disabled) {
        jdbcTemplate.update("UPDATE app_users SET is_disabled = ? WHERE id = ?", disabled, userId);
    }

    public void updateRoleName(final long userId, final String roleName) {
        jdbcTemplate.update("UPDATE app_users SET role = ? WHERE id = ?", roleName, userId);
    }

    public void setEmailVerified(final long userId, final boolean verified) {
        jdbcTemplate.update("UPDATE app_users SET email_verified = ? WHERE id = ?", verified, userId);
    }

    public void setMfa(final long userId, final boolean enabled, final String method) {
        jdbcTemplate.update("UPDATE app_users SET mfa_enabled = ?, mfa_method = ? WHERE id = ?", enabled, method, userId);
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
        if (rows.isEmpty()) return Optional.empty();
        final String s = rows.get(0);
        return (s == null || s.isBlank()) ? Optional.empty() : Optional.of(s);
    }

    private static Long extractGeneratedId(final KeyHolder keyHolder) {
        final List<Map<String, Object>> keyList = keyHolder.getKeyList();
        if (keyList.isEmpty()) return null;
        final Map<String, Object> row = keyList.get(0);

        Object idObj = row.containsKey("id") ? row.get("id") : row.get("ID");
        if (idObj instanceof Number n) return n.longValue();

        for (Object v : row.values()) {
            if (v instanceof Number n2) return n2.longValue();
        }
        return null;
    }

    public void updatePasswordHash(final long userId, final String passwordHash) {
        jdbcTemplate.update("UPDATE app_users SET password_hash = ? WHERE id = ?", passwordHash, userId);
    }

    public record UserRow(
            long id,
            String username,
            String passwordHash,
            Role role,
            boolean disabled,
            boolean emailVerified,
            boolean mfaEnabled,
            String mfaMethod,
            String totpSecret
    ) {}
}