package id.ac.ui.cs.advprog.backend.auth.repository;

import id.ac.ui.cs.advprog.backend.auth.model.Role;
import java.sql.PreparedStatement;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class UserAuthRepository {

    private static final String BASE_SELECT = """
            SELECT id, username, password_hash, role, is_disabled, email_verified, mfa_enabled, mfa_method,
                   totp_secret, legal_name, personal_key_hash, identity_doc_type, identity_doc_text,
                   personal_key_rotated_at
            FROM app_users
            """;

    private static final RowMapper<UserRow> USER_ROW_MAPPER = (rs, n) -> new UserRow(
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

    private final JdbcTemplate jdbcTemplate;

    public UserAuthRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UserRow> findByUsername(final String username) {
        return jdbcTemplate.query(BASE_SELECT + " WHERE username = ?", USER_ROW_MAPPER, username)
                .stream()
                .findFirst();
    }

    public Optional<UserRow> findById(final long id) {
        return jdbcTemplate.query(BASE_SELECT + " WHERE id = ?", USER_ROW_MAPPER, id)
                .stream()
                .findFirst();
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

        return requireGeneratedId(keyHolder);
    }

    public long insertVerifiedIdentityUser(final NewVerifiedIdentityUser user) {
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
            ps.setString(1, user.username());
            ps.setString(2, user.passwordHash());
            ps.setString(3, user.role().name());
            ps.setString(4, user.legalName());
            ps.setString(5, user.legalName());
            ps.setString(6, user.personalKeyHash());
            ps.setString(7, user.identityDocType());
            ps.setString(8, user.identityDocText());
            ps.setObject(9, OffsetDateTime.ofInstant(user.personalKeyRotatedAt(), ZoneOffset.UTC));
            return ps;
        }, keyHolder);

        return requireGeneratedId(keyHolder);
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

    private long requireGeneratedId(final KeyHolder keyHolder) {
        for (Map<String, Object> row : keyHolder.getKeyList()) {
            final Object lower = row.get("id");
            if (lower instanceof Number lowerNumber) {
                return lowerNumber.longValue();
            }

            final Object upper = row.get("ID");
            if (upper instanceof Number upperNumber) {
                return upperNumber.longValue();
            }

            for (Object value : row.values()) {
                if (value instanceof Number number) {
                    return number.longValue();
                }
            }
        }
        throw new IllegalStateException("failed_to_insert_user");
    }

    public record NewVerifiedIdentityUser(
            String username,
            String passwordHash,
            Role role,
            String legalName,
            String personalKeyHash,
            String identityDocType,
            String identityDocText,
            Instant personalKeyRotatedAt
    ) {}

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