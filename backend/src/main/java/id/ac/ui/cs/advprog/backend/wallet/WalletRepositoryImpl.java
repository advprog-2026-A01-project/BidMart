package id.ac.ui.cs.advprog.backend.wallet;

import org.springframework.stereotype.Repository;
import java.util.HashMap;
import java.util.Map;

@Repository 
public class WalletRepositoryImpl implements WalletRepository {
    private final Map<String, WalletEntity> database = new HashMap<>();

    @Override
    public WalletEntity findByUserId(String userId) {
        return database.get(userId);
    }

    @Override
    public WalletEntity save(WalletEntity wallet) {
        database.put(wallet.getUserId(), wallet);
        return wallet; 
    }
}