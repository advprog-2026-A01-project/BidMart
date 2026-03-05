package id.ac.ui.cs.advprog.backend.auth.api;

import java.util.Locale;
import java.util.Map;

import id.ac.ui.cs.advprog.backend.auth.model.AuthPrincipal;
import id.ac.ui.cs.advprog.backend.auth.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me")
public class UserSelfController {

    private static final String ERROR_KEY = "error";
    private static final String ERROR_UNAUTHORIZED = "unauthorized";
    private static final String ROLE_BUYER = "BUYER";
    private static final String ROLE_SELLER = "SELLER";
    private final UserRepository userRepository;

    public UserSelfController(final UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/become-seller")
    public ResponseEntity<?> becomeSeller(final Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal p)) {
            return ResponseEntity.status(401).body(Map.of(ERROR_KEY, ERROR_UNAUTHORIZED));
        }

        final String role = (p.role() == null || p.role().isBlank()) ? ROLE_BUYER : p.role().trim().toUpperCase(Locale.ROOT);
        if (ROLE_BUYER.equals(role)) {
            userRepository.updateRoleName(p.userId(), ROLE_SELLER);
        }

        return ResponseEntity.ok(Map.of("ok", true));
    }
}