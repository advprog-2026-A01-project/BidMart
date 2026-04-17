package id.ac.ui.cs.advprog.backend.wallet;

import lombok.Getter;
import lombok.Setter;
import java.time.ZonedDateTime;
import java.util.UUID;

@Getter
@Setter
public class WalletTransaction {
    private UUID id;
    private long userId;
    private String type;
    private double amount;
    private ZonedDateTime createdAt;

    public WalletTransaction(UUID id, long userId, String type, double amount, ZonedDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.amount = amount;
        this.createdAt = createdAt;
    }
}