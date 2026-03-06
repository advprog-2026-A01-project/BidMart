package id.ac.ui.cs.advprog.backend.auth.repository;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserProfileRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserProfileRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UserProfile getProfile(final long userId) {
        final var rows = jdbcTemplate.query(
                """
                SELECT display_name, photo_url, shipping_address
                FROM app_users
                WHERE id = ?
                """,
                (rs, n) -> new UserProfile(
                        rs.getString("display_name"),
                        rs.getString("photo_url"),
                        rs.getString("shipping_address")
                ),
                userId
        );
        return rows.isEmpty() ? new UserProfile(null, null, null) : rows.get(0);
    }

    public void updateProfile(final long userId, final UserProfile p) {
        jdbcTemplate.update(
                "UPDATE app_users SET display_name = ?, photo_url = ?, shipping_address = ? WHERE id = ?",
                p.displayName(), p.photoUrl(), p.shippingAddress(), userId
        );
    }

    public PublicProfile getPublicProfile(final long userId) {
        final var rows = jdbcTemplate.query(
                "SELECT id, username, display_name, photo_url, role FROM app_users WHERE id = ?",
                (rs, n) -> new PublicProfile(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("display_name"),
                        rs.getString("photo_url"),
                        rs.getString("role")
                ),
                userId
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    public record UserProfile(String displayName, String photoUrl, String shippingAddress) {}
    public record PublicProfile(long id, String username, String displayName, String photoUrl, String role) {}
}