package id.ac.ui.cs.advprog.wallet;

import id.ac.ui.cs.advprog.backend.wallet.WalletService;
import id.ac.ui.cs.advprog.backend.wallet.WalletRepository;
import id.ac.ui.cs.advprog.backend.wallet.WalletRepositoryImpl;
import id.ac.ui.cs.advprog.backend.wallet.WalletTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class WalletServiceTest {

    private WalletService walletService;
    private WalletRepository walletRepository;
    private final String userId = "nadine123";

    @BeforeEach
    void setUp() {
        walletRepository = new WalletRepositoryImpl();
        walletService = new WalletService(walletRepository);
    }

    @Test
    void testTopUpNegativeAmountShouldFail() {
        assertThrows(IllegalArgumentException.class, () -> {
            walletService.topUp(userId, -100.0);
        });
    }

    @Test
    void testHoldMoreThanAvailableBalanceShouldFail() {
        walletService.topUp(userId, 500.0);
        assertThrows(IllegalArgumentException.class, () -> {
            walletService.holdForBid(userId, 600.0);
        });
    }

    @Test
    void testFinalizePaymentFromHeldFunds() {
        walletService.topUp(userId, 1000.0);
        walletService.holdForBid(userId, 800.0);
        
        walletService.payFromHeld(userId, 800.0);
        
        assertEquals(200.0, walletService.getBalance(userId));
        assertEquals(0.0, walletService.getHeldBalance(userId));
    }

    @Test
    void testTransactionHistoryAuditTrail() {
        walletService.topUp(userId, 1000.0);
        walletService.holdForBid(userId, 200.0);
        walletService.releaseFromBid(userId, 200.0);
        
        List<WalletTransaction> history = walletService.getHistory(userId);
        assertEquals(3, history.size());
        assertEquals("TOPUP", history.get(0).getType());
        assertEquals("HOLD", history.get(1).getType());
        assertEquals("RELEASE", history.get(2).getType());
    }

    @Test
    void testGetBalanceForNewUserShouldReturnZero() {
        double balance = walletService.getBalance("new_user");
        assertEquals(0.0, balance);
    }
}