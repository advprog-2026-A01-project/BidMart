package id.ac.ui.cs.advprog.backend.wallet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class WalletService {
    private final WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    public WalletEntity getWallet(long userId) {
        return walletRepository.getOrCreateWallet(userId);
    }

    @Transactional
    public void topUp(long userId, double amount) {
        walletRepository.getOrCreateWallet(userId);
        if (!walletRepository.addBalance(userId, amount)) {
            throw new IllegalStateException("Failed to top up wallet");
        }
        WalletTransaction tx = new WalletTransaction(UUID.randomUUID(), userId, "TOPUP", amount, ZonedDateTime.now());
        walletRepository.insertTransaction(tx);
    }

    @Transactional
    public void withdraw(long userId, double amount) {
        if (!walletRepository.withdraw(userId, amount)) {
            throw new IllegalArgumentException("Insufficient balance for withdrawal or invalid amount");
        }
        WalletTransaction tx = new WalletTransaction(UUID.randomUUID(), userId, "WITHDRAW", amount, ZonedDateTime.now());
        walletRepository.insertTransaction(tx);
    }

    @Transactional
    public void holdForBid(long userId, double amount) {
        if (!walletRepository.holdFunds(userId, amount)) {
            throw new IllegalArgumentException("Insufficient balance to hold for bid");
        }
        WalletTransaction tx = new WalletTransaction(UUID.randomUUID(), userId, "HOLD", amount, ZonedDateTime.now());
        walletRepository.insertTransaction(tx);
    }

    @Transactional
    public void releaseFromBid(long userId, double amount) {
        if (!walletRepository.releaseFunds(userId, amount)) {
            throw new IllegalArgumentException("Insufficient held funds to release");
        }
        WalletTransaction tx = new WalletTransaction(UUID.randomUUID(), userId, "RELEASE", amount, ZonedDateTime.now());
        walletRepository.insertTransaction(tx);
    }

    @Transactional
    public void payFromHeld(long userId, double amount) {
        if (!walletRepository.convertToPayment(userId, amount)) {
            throw new IllegalArgumentException("Insufficient held funds for payment");
        }
        WalletTransaction tx = new WalletTransaction(UUID.randomUUID(), userId, "PAYMENT", amount, ZonedDateTime.now());
        walletRepository.insertTransaction(tx);
    }

    public List<WalletTransaction> getHistory(long userId) {
        return walletRepository.getTransactions(userId);
    }
}