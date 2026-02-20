package id.ac.ui.cs.advprog.backend.auth;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final SessionRepository sessionRepository;

    public AuthController(AuthService authService, SessionRepository sessionRepository) {
        this.authService = authService;
        this.sessionRepository = sessionRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Credentials body) {
        authService.register(body.username(), body.password());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Credentials body) {
        UUID token = authService.login(body.username(), body.password());
        return ResponseEntity.ok(Map.of("token", token.toString()));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "unauthorized"));
        return ResponseEntity.ok(Map.of("username", principal.getName()));
    }

    public record Credentials(String username, String password) {}
}
