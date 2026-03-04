package id.ac.ui.cs.advprog.backend.Wallet;
import org.springframework.stereotype.Service;

@Service
public class WalletService {

    private final WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    public double getBalance(String userId) {
        WalletEntity wallet = walletRepository.findByUserId(userId);
        if (wallet == null) {
            throw new IllegalArgumentException("Wallet not found for user: " + userId);
        }
        return wallet.getBalance();
    }

    public double topUp(String userId, double amount) {
        WalletEntity wallet = walletRepository.findByUserId(userId);
        if (wallet == null) {
            wallet = new WalletEntity(userId, 0.0, 0.0);
        }
        
        wallet.addBalance(amount);
        walletRepository.save(wallet); 
        return wallet.getBalance();
    }

    public boolean validateForBidding(String userId, double bidAmount) {
        WalletEntity wallet = walletRepository.findByUserId(userId);
        if (wallet == null) {
            throw new IllegalArgumentException("Wallet not found.");
        }
        
        if (!wallet.canAfford(bidAmount)) {
            throw new IllegalArgumentException("Insufficient balance to place a bid.");
        }
        return true;
    }
}