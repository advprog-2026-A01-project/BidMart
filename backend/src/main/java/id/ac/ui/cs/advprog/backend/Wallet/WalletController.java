package id.ac.ui.cs.advprog.backend.Wallet;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/{userId}/balance")
    public ResponseEntity<Double> getBalance(@PathVariable String userId) {
        return ResponseEntity.ok(walletService.getBalance(userId));
    }

    @PostMapping("/{userId}/topup")
    public ResponseEntity<String> topUp(@PathVariable String userId, @RequestBody Map<String, Double> payload) {
        double amount = payload.get("amount");
        double newBalance = walletService.topUp(userId, amount);
        return ResponseEntity.ok("Top-up successful. Current balance: " + newBalance);
    }

    @GetMapping("/{userId}/validate")
    public ResponseEntity<Boolean> validate(@PathVariable String userId, @RequestParam double amount) {
        return ResponseEntity.ok(walletService.validateForBidding(userId, amount));
    }
}