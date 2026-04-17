package id.ac.ui.cs.advprog.backend.wallet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class WalletServiceTest {

    private WalletRepository repository;
    private WalletService service;

    @BeforeEach
    void setUp() {
        repository = mock(WalletRepository.class);
        service = new WalletService(repository);
    }

    @Test
    void testTopUp() {
        when(repository.addBalance(1L, 100.0)).thenReturn(true);
        assertDoesNotThrow(() -> service.topUp(1L, 100.0));
        verify(repository).insertTransaction(any(WalletTransaction.class));
    }

    @Test
    void testTopUpFails() {
        when(repository.addBalance(1L, 100.0)).thenReturn(false);
        assertThrows(IllegalStateException.class, () -> service.topUp(1L, 100.0));
        verify(repository, never()).insertTransaction(any(WalletTransaction.class));
    }

    @Test
    void testWithdraw() {
        when(repository.withdraw(1L, 50.0)).thenReturn(true);
        assertDoesNotThrow(() -> service.withdraw(1L, 50.0));
        verify(repository).insertTransaction(argThat(tx -> tx.getType().equals("WITHDRAW")));
    }

    @Test
    void testWithdrawInsufficientBalance() {
        when(repository.withdraw(1L, 50.0)).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> service.withdraw(1L, 50.0));
    }

    @Test
    void testHoldForBid() {
        when(repository.holdFunds(1L, 20.0)).thenReturn(true);
        assertDoesNotThrow(() -> service.holdForBid(1L, 20.0));
        verify(repository).insertTransaction(argThat(tx -> tx.getType().equals("HOLD")));
    }

    @Test
    void testPayFromHeld() {
        when(repository.convertToPayment(1L, 20.0)).thenReturn(true);
        assertDoesNotThrow(() -> service.payFromHeld(1L, 20.0));
        verify(repository).insertTransaction(argThat(tx -> tx.getType().equals("PAYMENT")));
    }
}
