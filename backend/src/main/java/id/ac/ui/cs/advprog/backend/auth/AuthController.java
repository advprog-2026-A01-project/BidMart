package id.ac.ui.cs.advprog.backend.auth;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
Tanggung jawab: boundary HTTP (DTO, status code, mapping request/response).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final SessionRepository sessionRepository;
    private final AuthProperties props;

    public AuthController(
            final AuthService authService,
            final SessionRepository sessionRepository,
            final AuthProperties props
    ) {
        this.authService = authService;
        this.sessionRepository = sessionRepository;
        this.props = props;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody final RegisterRequest body) {
        final String username = normalizeUsername(body.username());
        if (username.isBlank() || body.password() == null || body.password().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid_input"));
        }
        authService.register(username, body.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("ok", true));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody final LoginRequest body, final HttpServletRequest request) {
        final String username = normalizeUsername(body.username());
        final var tokenPair = authService.login(username, body.password(), request.getHeader("User-Agent"), request.getRemoteAddr());
        return ResponseEntity.ok(toTokenResponse(tokenPair));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody final RefreshRequest body, final HttpServletRequest request) {
        final var tokenPair = authService.refresh(body.refreshToken(), request.getHeader("User-Agent"), request.getRemoteAddr());
        return ResponseEntity.ok(toTokenResponse(tokenPair));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(final Authentication authentication) {
        if (authentication == null || authentication.getDetails() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "unauthorized"));
        }
        final String accessToken = String.valueOf(authentication.getDetails());
        authService.logout(accessToken);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(final Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal p)) {
            return ResponseEntity.status(401).body(Map.of("error", "unauthorized"));
        }
        return ResponseEntity.ok(new MeResponse(p.username(), p.role().name()));
    }

    @GetMapping("/sessions")
    public ResponseEntity<?> sessions(final Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal p)) {
            return ResponseEntity.status(401).body(Map.of("error", "unauthorized"));
        }
        final List<SessionRepository.SessionRow> sessions = sessionRepository.listSessions(p.userId());
        return ResponseEntity.ok(sessions);
    }

    private TokenResponse toTokenResponse(final SessionRepository.TokenPair pair) {
        final long expiresInSec = Duration.ofMinutes(props.getAccessTtlMinutes()).toSeconds();
        return new TokenResponse(
                pair.accessToken().toString(),
                pair.refreshToken().toString(),
                "Bearer",
                expiresInSec
        );
    }

    private String normalizeUsername(final String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }

    public record RegisterRequest(
            @JsonAlias({"email"})
            String username,
            String password
    ) {}

    public record LoginRequest(
            @JsonAlias({"email"})
            String username,
            String password
    ) {}

    public record RefreshRequest(String refreshToken) {}

    public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {}

    public record MeResponse(String username, String role) {}
}