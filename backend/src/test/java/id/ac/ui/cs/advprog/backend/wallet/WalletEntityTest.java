package id.ac.ui.cs.advprog.backend.wallet;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class WalletEntityTest {

    @Test
    void testWalletEntityInitialization() {
        WalletEntity wallet = new WalletEntity(1L, 100.0, 50.0);
        assertEquals(1L, wallet.getUserId());
        assertEquals(100.0, wallet.getBalance());
        assertEquals(50.0, wallet.getHeldBalance());
    }

    @Test
    void testWalletEntitySetters() {
        WalletEntity wallet = new WalletEntity(1L, 0.0, 0.0);
        wallet.setBalance(150.0);
        wallet.setHeldBalance(25.0);
        assertEquals(150.0, wallet.getBalance());
        assertEquals(25.0, wallet.getHeldBalance());
    }
}
