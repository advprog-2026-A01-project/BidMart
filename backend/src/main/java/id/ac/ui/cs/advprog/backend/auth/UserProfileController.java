package id.ac.ui.cs.advprog.backend.auth;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserProfileController {

    private static final String ERROR_KEY = "error";
    private final UserRepository userRepository;

    public UserProfileController(final UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me/profile")
    public ResponseEntity<?> getMyProfile(final Authentication authentication) {
        final AuthPrincipal p = requirePrincipal(authentication);
        if (p == null) return ResponseEntity.status(401).body(Map.of(ERROR_KEY, "unauthorized"));
        return ResponseEntity.ok(userRepository.getProfile(p.userId()));
    }

    @PutMapping("/me/profile")
    public ResponseEntity<?> updateMyProfile(final Authentication authentication, @RequestBody final UserRepository.UserProfile body) {
        final AuthPrincipal p = requirePrincipal(authentication);
        if (p == null) return ResponseEntity.status(401).body(Map.of(ERROR_KEY, "unauthorized"));

        final var safe = new UserRepository.UserProfile(
                body == null ? null : body.displayName(),
                body == null ? null : body.photoUrl(),
                body == null ? null : body.shippingAddress()
        );

        userRepository.updateProfile(p.userId(), safe);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    // Public profile for catalog/listing (no shipping address)
    @GetMapping("/{id}/public-profile")
    public ResponseEntity<?> publicProfile(@PathVariable("id") final long id) {
        final var p = userRepository.getPublicProfile(id);
        if (p == null) return ResponseEntity.status(404).body(Map.of(ERROR_KEY, "not_found"));
        return ResponseEntity.ok(p);
    }

    private static AuthPrincipal requirePrincipal(final Authentication authentication) {
        if (authentication == null) return null;
        if (!(authentication.getPrincipal() instanceof AuthPrincipal p)) return null;
        return p;
    }
}