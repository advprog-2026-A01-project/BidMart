package id.ac.ui.cs.advprog.backend.wallet;

public interface WalletRepository {
    WalletEntity findByUserId(String userId);
    WalletEntity save(WalletEntity wallet);
}