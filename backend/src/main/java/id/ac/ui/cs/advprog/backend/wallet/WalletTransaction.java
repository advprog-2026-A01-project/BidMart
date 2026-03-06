package id.ac.ui.cs.advprog.backend.wallet;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class WalletTransaction {
    private String transactionId;
    private String userId;
    private String type; 
    private double amount;
    private LocalDateTime timestamp;

    public WalletTransaction(String userId, String type, double amount) {
        this.transactionId = java.util.UUID.randomUUID().toString();
        this.userId = userId;
        this.type = type;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
    }
}