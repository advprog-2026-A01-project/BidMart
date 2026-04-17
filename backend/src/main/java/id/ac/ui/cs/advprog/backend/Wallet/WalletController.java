package id.ac.ui.cs.advprog.backend.wallet;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import id.ac.ui.cs.advprog.backend.auth.model.AuthPrincipal;

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

    private long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthPrincipal user) {
            return user.userId();
        }
        throw new IllegalStateException("User not authenticated");
    }

    private void verifyOwnership(long pathUserId) {
        if (pathUserId != getCurrentUserId()) {
            throw new SecurityException("Not authorized to access this wallet");
        }
    }

    @GetMapping("/me/info")
    public ResponseEntity<Map<String, Double>> getMyWalletInfo() {
        long userId = getCurrentUserId();
        WalletEntity wallet = walletService.getWallet(userId);
        return ResponseEntity.ok(Map.of(
            "availableBalance", wallet.getBalance(),
            "heldBalance", wallet.getHeldBalance()
        ));
    }

    @PostMapping("/me/topup")
    public ResponseEntity<String> myTopUp(@RequestBody Map<String, Double> payload) {
        long userId = getCurrentUserId();
        walletService.topUp(userId, payload.get(AMOUNT_KEY));
        return ResponseEntity.ok("Top-up successful");
    }

    @PostMapping("/me/withdraw")
    public ResponseEntity<String> myWithdraw(@RequestBody Map<String, Double> payload) {
        long userId = getCurrentUserId();
        try {
            walletService.withdraw(userId, payload.get(AMOUNT_KEY));
            return ResponseEntity.ok("Withdrawal successful");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/me/history")
    public ResponseEntity<List<WalletTransaction>> getMyHistory() {
        long userId = getCurrentUserId();
        return ResponseEntity.ok(walletService.getHistory(userId));
    }

    @PostMapping("/{userId}/bid/hold")
    public ResponseEntity<String> holdForBid(@PathVariable long userId, @RequestBody Map<String, Double> payload) {
        try {
            walletService.holdForBid(userId, payload.get(AMOUNT_KEY));
            return ResponseEntity.ok("Funds held for bid");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{userId}/bid/release")
    public ResponseEntity<String> releaseFromBid(@PathVariable long userId, @RequestBody Map<String, Double> payload) {
        try {
            walletService.releaseFromBid(userId, payload.get(AMOUNT_KEY));
            return ResponseEntity.ok("Funds released");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{userId}/bid/pay")
    public ResponseEntity<String> payForWin(@PathVariable long userId, @RequestBody Map<String, Double> payload) {
        try {
            walletService.payFromHeld(userId, payload.get(AMOUNT_KEY));
            return ResponseEntity.ok("Payment successful from held funds");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}