package id.ac.ui.cs.advprog.wallet;

import id.ac.ui.cs.advprog.backend.wallet.WalletEntity;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WalletEntityTest {

    @Test
    void testWithdrawSuccess() {
        WalletEntity wallet = new WalletEntity("user1", 1000.0, 0.0);
        wallet.withdraw(500.0);
        assertEquals(500.0, wallet.getBalance());
    }

    @Test
    void testWithdrawInsufficientShouldFail() {
        WalletEntity wallet = new WalletEntity("user1", 100.0, 0.0);
        assertThrows(IllegalArgumentException.class, () -> wallet.withdraw(200.0));
    }

    @Test
    void testReleaseFundsMoreThanHeldShouldFail() {
        WalletEntity wallet = new WalletEntity("user1", 0.0, 100.0);
        assertThrows(IllegalArgumentException.class, () -> wallet.releaseFunds(150.0));
    }
}