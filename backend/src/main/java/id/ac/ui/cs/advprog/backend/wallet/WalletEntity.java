package id.ac.ui.cs.advprog.backend.wallet;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WalletEntity {
    private long userId;
    private double balance;     
    private double heldBalance;  

    public WalletEntity(long userId, double balance, double heldBalance) {
        this.userId = userId;
        this.balance = balance;
        this.heldBalance = heldBalance;
    }
}