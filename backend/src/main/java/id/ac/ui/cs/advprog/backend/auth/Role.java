package id.ac.ui.cs.advprog.backend.auth;

/*
Tanggung jawab: definisi peran minimal yang stabil untuk semua modul.
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
        } catch (IllegalArgumentException e) {
            return BUYER;
        }
    }

}