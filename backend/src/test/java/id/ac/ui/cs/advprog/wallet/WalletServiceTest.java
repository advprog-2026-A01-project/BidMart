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
        }, "Top up with negative amount should throw IllegalArgumentException");
    }

    @Test
    void testHoldMoreThanAvailableBalanceShouldFail() {
        walletService.topUp(userId, 500.0);
        assertThrows(IllegalArgumentException.class, () -> {
            walletService.holdForBid(userId, 600.0);
        }, "Holding more than available balance should throw IllegalArgumentException");
    }


    @Test
    void testFinalizePaymentShouldUpdateAvailableBalance() {
        walletService.topUp(userId, 1000.0);
        walletService.holdForBid(userId, 800.0);
        walletService.payFromHeld(userId, 800.0);
        
        assertEquals(200.0, walletService.getBalance(userId), 
            "Available balance should be original minus payment");
    }

    @Test
    void testFinalizePaymentShouldClearHeldBalance() {
        walletService.topUp(userId, 1000.0);
        walletService.holdForBid(userId, 800.0);
        walletService.payFromHeld(userId, 800.0);
        
        assertEquals(0.0, walletService.getHeldBalance(userId), 
            "Held balance should be empty after payment finalized");
    }

    @Test
    void testTransactionHistorySize() {
        walletService.topUp(userId, 1000.0);
        walletService.holdForBid(userId, 200.0);
        walletService.releaseFromBid(userId, 200.0);
        
        List<WalletTransaction> history = walletService.getHistory(userId);
        assertEquals(3, history.size(), "History should contain 3 entries");
    }

    @Test
    void testTransactionHistoryFirstEntryIsTopUp() {
        walletService.topUp(userId, 1000.0);
        List<WalletTransaction> history = walletService.getHistory(userId);
        assertEquals("TOPUP", history.get(0).getType(), "First transaction should be TOPUP");
    }

    @Test
    void testTransactionHistorySecondEntryIsHold() {
        walletService.topUp(userId, 1000.0);
        walletService.holdForBid(userId, 200.0);
        List<WalletTransaction> history = walletService.getHistory(userId);
        assertEquals("HOLD", history.get(1).getType(), "Second transaction should be HOLD");
    }

    @Test
    void testGetBalanceForNewUserShouldReturnZero() {
        double balance = walletService.getBalance("new_user");
        assertEquals(0.0, balance, "New user balance should be zero by default");
    }
}