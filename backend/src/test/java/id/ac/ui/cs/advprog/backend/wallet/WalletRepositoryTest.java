package id.ac.ui.cs.advprog.backend.wallet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class WalletRepositoryTest {

    @org.springframework.beans.factory.annotation.Autowired
    private JdbcTemplate jdbcTemplate;

    private WalletRepository repository;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.sql.init.mode", () -> "always");
    }

    @BeforeEach
    void setUp() {
        repository = new WalletRepository(jdbcTemplate);
        jdbcTemplate.update("INSERT INTO app_users(id, username, password_hash, role) VALUES (1, 'tester', 'hash', 'BUYER') ON CONFLICT DO NOTHING");
    }

    @Test
    void testGetOrCreateWallet() {
        WalletEntity w1 = repository.getOrCreateWallet(1L);
        assertEquals(1L, w1.getUserId());
        assertEquals(0.0, w1.getBalance());

        jdbcTemplate.update("UPDATE app_wallets SET balance = 50.0 WHERE user_id = 1");

        WalletEntity w2 = repository.getOrCreateWallet(1L);
        assertEquals(50.0, w2.getBalance());
    }

    @Test
    void testAddBalance() {
        repository.getOrCreateWallet(1L);
        assertTrue(repository.addBalance(1L, 100.0));
        
        WalletEntity w = repository.getOrCreateWallet(1L);
        assertEquals(100.0, w.getBalance());
    }

    @Test
    void testWithdrawSucceedsAndFailsCorrectly() {
        repository.getOrCreateWallet(1L);
        repository.addBalance(1L, 100.0);
        
        assertTrue(repository.withdraw(1L, 60.0));
        WalletEntity w = repository.getOrCreateWallet(1L);
        assertEquals(40.0, w.getBalance());

        assertFalse(repository.withdraw(1L, 50.0));
        w = repository.getOrCreateWallet(1L);
        assertEquals(40.0, w.getBalance());
    }

    @Test
    void testHoldAndReleaseFunds() {
        repository.getOrCreateWallet(1L);
        repository.addBalance(1L, 100.0);

        assertTrue(repository.holdFunds(1L, 50.0));
        WalletEntity w = repository.getOrCreateWallet(1L);
        assertEquals(50.0, w.getBalance());
        assertEquals(50.0, w.getHeldBalance());

        assertTrue(repository.releaseFunds(1L, 25.0));
        w = repository.getOrCreateWallet(1L);
        assertEquals(75.0, w.getBalance());
        assertEquals(25.0, w.getHeldBalance());
    }

    @Test
    void testConvertToPayment() {
        repository.getOrCreateWallet(1L);
        repository.addBalance(1L, 100.0);
        repository.holdFunds(1L, 50.0);

        assertTrue(repository.convertToPayment(1L, 50.0));
        WalletEntity w = repository.getOrCreateWallet(1L);
        assertEquals(50.0, w.getBalance());
        assertEquals(0.0, w.getHeldBalance());
    }

    @Test
    void testTransactionHistory() {
        repository.getOrCreateWallet(1L);
        repository.insertTransaction(new WalletTransaction(UUID.randomUUID(), 1L, "TOPUP", 200.0, ZonedDateTime.now()));
        List<WalletTransaction> txs = repository.getTransactions(1L);
        assertEquals(1, txs.size());
        assertEquals(200.0, txs.get(0).getAmount());
        assertEquals("TOPUP", txs.get(0).getType());
    }
}
