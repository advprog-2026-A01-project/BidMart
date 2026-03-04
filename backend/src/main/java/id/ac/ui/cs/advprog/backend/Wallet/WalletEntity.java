package id.ac.ui.cs.advprog.backend.Wallet;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WalletEntity {
    private String userId;
    private double balance;
    private double heldBalance;

    public WalletEntity(String userId, double balance, double heldBalance) {
        this.userId = userId;
        this.balance = balance;
        this.heldBalance = heldBalance;
    }

    public boolean canAfford(double amount) {
        return this.balance >= amount;
    }

    public void addBalance(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Top-up amount must be positive");
        }
        this.balance += amount;
    }
}