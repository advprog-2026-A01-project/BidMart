package id.ac.ui.cs.advprog.backend.wallet;

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

    public void addBalance(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        this.balance += amount;
    }

    public void withdraw(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        if (this.balance < amount) throw new IllegalArgumentException("Insufficient balance");
        this.balance -= amount;
    }

    public void holdFunds(double amount) {
        if (this.balance < amount) throw new IllegalArgumentException("Insufficient balance to hold");
        this.balance -= amount;
        this.heldBalance += amount;
    }

    public void releaseFunds(double amount) {
        if (this.heldBalance < amount) throw new IllegalArgumentException("Insufficient held funds");
        this.heldBalance -= amount;
        this.balance += amount;
    }

    public void convertToPayment(double amount) {
        if (this.heldBalance < amount) throw new IllegalArgumentException("Insufficient held funds for payment");
        this.heldBalance -= amount;
    }
}