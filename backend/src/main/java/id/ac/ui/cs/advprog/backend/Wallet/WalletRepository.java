package id.ac.ui.cs.advprog.backend.Wallet;

public interface WalletRepository {
    WalletEntity findByUserId(String userId);
    WalletEntity save(WalletEntity wallet);
}