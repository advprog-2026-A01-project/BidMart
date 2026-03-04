package id.ac.ui.cs.advprog.backend.auth;

/**
 * Base roles (will be extended to dynamic roles/permissions in later milestones).
 */
public enum Role {
    ADMIN,
    SELLER,
    BUYER;

    public static Role fromDb(final String value) {
        if (value == null)
            return BUYER;
        try {
            return Role.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return BUYER;
        }
    }
}