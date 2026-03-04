package id.ac.ui.cs.advprog.backend.auth;

import java.util.Locale;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me")
public class UserSelfController {

    private static final String ERROR_KEY = "error";
    private final UserRepository userRepository;

    public UserSelfController(final UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/become-seller")
    public ResponseEntity<?> becomeSeller(final Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal p)) {
            return ResponseEntity.status(401).body(Map.of(ERROR_KEY, "unauthorized"));
        }

        final String role = p.role() == null ? "BUYER" : p.role().toUpperCase(Locale.ROOT);
        if ("BUYER".equals(role)) {
            userRepository.updateRoleName(p.userId(), "SELLER");
        }

        return ResponseEntity.ok(Map.of("ok", true));
    }
}