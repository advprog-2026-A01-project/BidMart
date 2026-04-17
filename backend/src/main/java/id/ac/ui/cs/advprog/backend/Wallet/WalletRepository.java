package id.ac.ui.cs.advprog.backend.wallet;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Repository
public class WalletRepository {
    private final JdbcTemplate jdbcTemplate;

    public WalletRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public WalletEntity getOrCreateWallet(long userId) {
        List<WalletEntity> wallets = jdbcTemplate.query(
            "SELECT user_id, balance, held_balance FROM app_wallets WHERE user_id = ?",
            (rs, rowNum) -> new WalletEntity(
                rs.getLong("user_id"),
                rs.getDouble("balance"),
                rs.getDouble("held_balance")
            ),
            userId
        );

        if (wallets.isEmpty()) {
            jdbcTemplate.update(
                "INSERT INTO app_wallets (user_id, balance, held_balance) VALUES (?, 0.0, 0.0) ON CONFLICT DO NOTHING",
                userId
            );
            return new WalletEntity(userId, 0.0, 0.0);
        }
        return wallets.get(0);
    }

    public boolean addBalance(long userId, double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        int rows = jdbcTemplate.update(
            "UPDATE app_wallets SET balance = balance + ? WHERE user_id = ?",
            amount, userId
        );
        return rows > 0;
    }

    public boolean withdraw(long userId, double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        int rows = jdbcTemplate.update(
            "UPDATE app_wallets SET balance = balance - ? WHERE user_id = ? AND balance >= ?",
            amount, userId, amount
        );
        return rows > 0;
    }

    public boolean holdFunds(long userId, double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        int rows = jdbcTemplate.update(
            "UPDATE app_wallets SET balance = balance - ?, held_balance = held_balance + ? " +
            "WHERE user_id = ? AND balance >= ?",
            amount, amount, userId, amount
        );
        return rows > 0;
    }

    public boolean releaseFunds(long userId, double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        int rows = jdbcTemplate.update(
            "UPDATE app_wallets SET held_balance = held_balance - ?, balance = balance + ? " +
            "WHERE user_id = ? AND held_balance >= ?",
            amount, amount, userId, amount
        );
        return rows > 0;
    }

    public boolean convertToPayment(long userId, double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        int rows = jdbcTemplate.update(
            "UPDATE app_wallets SET held_balance = held_balance - ? " +
            "WHERE user_id = ? AND held_balance >= ?",
            amount, userId, amount
        );
        return rows > 0;
    }

    public void insertTransaction(WalletTransaction tx) {
        jdbcTemplate.update(
            "INSERT INTO app_wallet_transactions (id, user_id, type, amount, created_at) VALUES (?, ?, ?, ?, ?)",
            tx.getId(), tx.getUserId(), tx.getType(), tx.getAmount(), tx.getCreatedAt().toOffsetDateTime()
        );
    }

    public List<WalletTransaction> getTransactions(long userId) {
        return jdbcTemplate.query(
            "SELECT id, user_id, type, amount, created_at FROM app_wallet_transactions " +
            "WHERE user_id = ? ORDER BY created_at DESC",
            (rs, rowNum) -> {
                java.sql.Timestamp ts = rs.getTimestamp("created_at");
                ZonedDateTime zdt = ts != null ? ts.toInstant().atZone(ZoneId.systemDefault()) : null;
                return new WalletTransaction(
                    rs.getObject("id", UUID.class),
                    rs.getLong("user_id"),
                    rs.getString("type"),
                    rs.getDouble("amount"),
                    zdt
                );
            },
            userId
        );
    }
}