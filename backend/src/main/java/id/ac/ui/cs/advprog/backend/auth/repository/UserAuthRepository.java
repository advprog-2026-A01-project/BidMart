package id.ac.ui.cs.advprog.backend.auth.repository;

import id.ac.ui.cs.advprog.backend.auth.model.Role;
import java.sql.PreparedStatement;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
                baseSelect() + " WHERE username = ?",
                (rs, n) -> mapUserRow(rs),
                username
        );
        return rows.stream().findFirst();
    }

    public Optional<UserRow> findById(final long id) {
        final var rows = jdbcTemplate.query(
                baseSelect() + " WHERE id = ?",
                (rs, n) -> mapUserRow(rs),
                id
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

    public long insertVerifiedIdentityUser(
            final String username,
            final String passwordHash,
            final Role role,
            final String legalName,
            final String personalKeyHash,
            final String identityDocType,
            final String identityDocText,
            final Instant personalKeyRotatedAt
    ) {
        final KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            final PreparedStatement ps = con.prepareStatement(
                    """
                    INSERT INTO app_users(
                        username,
                        password_hash,
                        role,
                        email_verified,
                        legal_name,
                        display_name,
                        personal_key_hash,
                        identity_doc_type,
                        identity_doc_text,
                        personal_key_rotated_at
                    ) VALUES (?, ?, ?, TRUE, ?, ?, ?, ?, ?, ?)
                    """,
                    new String[] {"id"}
            );
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setString(3, role.name());
            ps.setString(4, legalName);
            ps.setString(5, legalName);
            ps.setString(6, personalKeyHash);
            ps.setString(7, identityDocType);
            ps.setString(8, identityDocText);
            ps.setObject(9, OffsetDateTime.ofInstant(personalKeyRotatedAt, ZoneOffset.UTC));
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

    private static String baseSelect() {
        return """
                SELECT id, username, password_hash, role, is_disabled, email_verified, mfa_enabled, mfa_method,
                       totp_secret, legal_name, personal_key_hash, identity_doc_type, identity_doc_text,
                       personal_key_rotated_at
                FROM app_users
                """;
    }

    private static UserRow mapUserRow(final java.sql.ResultSet rs) throws java.sql.SQLException {
        return new UserRow(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                Role.fromDb(rs.getString("role")),
                rs.getBoolean("is_disabled"),
                rs.getBoolean("email_verified"),
                rs.getBoolean("mfa_enabled"),
                rs.getString("mfa_method"),
                rs.getString("totp_secret"),
                rs.getString("legal_name"),
                rs.getString("personal_key_hash"),
                rs.getString("identity_doc_type"),
                rs.getString("identity_doc_text"),
                rs.getObject("personal_key_rotated_at", OffsetDateTime.class)
        );
    }

    private static Long extractGeneratedId(final KeyHolder keyHolder) {
        final List<Map<String, Object>> keyList = keyHolder.getKeyList();
        if (keyList.isEmpty()) return null;
        final Map<String, Object> row = keyHolder.getKeyList().get(0);

        Object idObj = row.containsKey("id") ? row.get("id") : row.get("ID");
        if (idObj instanceof Number n) return n.longValue();

        for (Object value : row.values()) {
            if (value instanceof Number n2) return n2.longValue();
        }
        return null;
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
            String totpSecret,
            String legalName,
            String personalKeyHash,
            String identityDocType,
            String identityDocText,
            OffsetDateTime personalKeyRotatedAt
    ) {}
}