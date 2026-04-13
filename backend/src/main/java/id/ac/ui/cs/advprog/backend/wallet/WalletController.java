package id.ac.ui.cs.advprog.backend.wallet;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {
    private static final String AMOUNT_KEY = "amount";
    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/{userId}/info")
    public ResponseEntity<Map<String, Double>> getWalletInfo(@PathVariable String userId) {
        return ResponseEntity.ok(Map.of(
            "availableBalance", walletService.getBalance(userId),
            "heldBalance", walletService.getHeldBalance(userId)
        ));
    }

    @PostMapping("/{userId}/topup")
    public ResponseEntity<String> topUp(@PathVariable String userId, @RequestBody Map<String, Double> payload) {
        walletService.topUp(userId, payload.get(AMOUNT_KEY));
        return ResponseEntity.ok("Top-up successful");
    }

    @PostMapping("/{userId}/bid/hold")
    public ResponseEntity<String> holdForBid(@PathVariable String userId, @RequestBody Map<String, Double> payload) {
        walletService.holdForBid(userId, payload.get(AMOUNT_KEY));
        return ResponseEntity.ok("Funds held for bid");
    }

    @PostMapping("/{userId}/bid/release")
    public ResponseEntity<String> releaseFromBid(@PathVariable String userId, @RequestBody Map<String, Double> payload) {
        walletService.releaseFromBid(userId, payload.get(AMOUNT_KEY));
        return ResponseEntity.ok("Funds released");
    }

    @PostMapping("/{userId}/bid/pay")
    public ResponseEntity<String> payForWin(@PathVariable String userId, @RequestBody Map<String, Double> payload) {
        walletService.payFromHeld(userId, payload.get(AMOUNT_KEY));
        return ResponseEntity.ok("Payment successful from held funds");
    }

    @GetMapping("/{userId}/history")
    public ResponseEntity<List<WalletTransaction>> getHistory(@PathVariable String userId) {
        return ResponseEntity.ok(walletService.getHistory(userId));
    }
}