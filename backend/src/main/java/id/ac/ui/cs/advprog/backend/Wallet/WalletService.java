package id.ac.ui.cs.advprog.backend.wallet;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WalletService {
    private final WalletRepository walletRepository;
    private final List<WalletTransaction> transactionHistory = new ArrayList<>(); // In-memory audit trail

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    private WalletEntity getOrCreateWallet(String userId) {
        WalletEntity wallet = walletRepository.findByUserId(userId);
        if (wallet == null) {
            wallet = new WalletEntity(userId, 0.0, 0.0);
            walletRepository.save(wallet);
        }
        return wallet;
    }

    public double getBalance(String userId) {
        return getOrCreateWallet(userId).getBalance();
    }

    public double getHeldBalance(String userId) {
        return getOrCreateWallet(userId).getHeldBalance();
    }

    public void topUp(String userId, double amount) {
        WalletEntity wallet = getOrCreateWallet(userId);
        wallet.addBalance(amount);
        walletRepository.save(wallet);
        transactionHistory.add(new WalletTransaction(userId, "TOPUP", amount));
    }

    public void withdraw(String userId, double amount) {
        WalletEntity wallet = getOrCreateWallet(userId);
        wallet.withdraw(amount);
        walletRepository.save(wallet);
        transactionHistory.add(new WalletTransaction(userId, "WITHDRAW", amount));
    }

    public void holdForBid(String userId, double amount) {
        WalletEntity wallet = getOrCreateWallet(userId);
        wallet.holdFunds(amount);
        walletRepository.save(wallet);
        transactionHistory.add(new WalletTransaction(userId, "HOLD", amount));
    }

    public void releaseFromBid(String userId, double amount) {
        WalletEntity wallet = getOrCreateWallet(userId);
        wallet.releaseFunds(amount);
        walletRepository.save(wallet);
        transactionHistory.add(new WalletTransaction(userId, "RELEASE", amount));
    }

    public void payFromHeld(String userId, double amount) {
        WalletEntity wallet = getOrCreateWallet(userId);
        wallet.convertToPayment(amount);
        walletRepository.save(wallet);
        transactionHistory.add(new WalletTransaction(userId, "PAYMENT", amount));
    }

    public List<WalletTransaction> getHistory(String userId) {
        return transactionHistory.stream()
                .filter(t -> t.getUserId().equalsIgnoreCase(userId))
                .collect(Collectors.toList());
    }
}