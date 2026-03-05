package id.ac.ui.cs.advprog.backend.auth.api;

import com.fasterxml.jackson.annotation.JsonAlias;
import id.ac.ui.cs.advprog.backend.auth.model.AuthProperties;
import id.ac.ui.cs.advprog.backend.auth.model.Role;
import id.ac.ui.cs.advprog.backend.auth.repository.SessionRepository;
import id.ac.ui.cs.advprog.backend.auth.service.AuthLoginService;
import id.ac.ui.cs.advprog.backend.auth.service.AuthRegistrationService;
import id.ac.ui.cs.advprog.backend.auth.service.AuthTokenService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthPublicController {

    private static final String ERROR_KEY = "error";

    private final AuthRegistrationService registrationService;
    private final AuthLoginService loginService;
    private final AuthTokenService tokenService;
    private final AuthProperties props;

    public AuthPublicController(
            final AuthRegistrationService registrationService,
            final AuthLoginService loginService,
            final AuthTokenService tokenService,
            final AuthProperties props
    ) {
        this.registrationService = registrationService;
        this.loginService = loginService;
        this.tokenService = tokenService;
        this.props = props;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody final RegisterRequest body) {
        final String username = normalizeUsername(body.username());
        final String password = (body.password() == null) ? "" : body.password();

        if (username.isBlank() || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "invalid_input"));
        }

        final Role role = parseRequestedRole(body.requestedRole());
        if (role == null) return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "invalid_role"));

        final var token = registrationService.register(username, password, role);
        return ResponseEntity.status(HttpStatus.CREATED).body(new RegisterResponse(true, token.toString()));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestBody final VerifyEmailRequest body) {
        final String token = (body.token() == null) ? "" : body.token().trim();
        if (token.isBlank()) return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "invalid_input"));
        registrationService.verifyEmail(token);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody final LoginRequest body, final HttpServletRequest request) {
        final String username = normalizeUsername(body.username());
        final var meta = new AuthLoginService.ClientMeta(request.getHeader("User-Agent"), request.getRemoteAddr());

        final var out = loginService.login(username, body.password(), meta);
        if (out instanceof AuthLoginService.LoginResult.Tokens t) {
            return ResponseEntity.ok(toTokenResponse(t.tokens()));
        }

        final AuthLoginService.LoginResult.MfaRequired m = (AuthLoginService.LoginResult.MfaRequired) out;
        return ResponseEntity.ok(new MfaChallengeResponse(
                true,
                m.challengeId().toString(),
                m.method(),
                m.expiresInSeconds(),
                m.devCode()
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody final RefreshRequest body, final HttpServletRequest request) {
        final var meta = new AuthTokenService.ClientMeta(request.getHeader("User-Agent"), request.getRemoteAddr());
        final SessionRepository.TokenPair pair = tokenService.refresh(body.refreshToken(), meta);
        return ResponseEntity.ok(toTokenResponse(pair));
    }

    private TokenResponse toTokenResponse(final SessionRepository.TokenPair pair) {
        final long expiresInSec = Duration.ofMinutes(props.getAccessTtlMinutes()).toSeconds();
        return new TokenResponse(pair.accessToken().toString(), pair.refreshToken().toString(), "Bearer", expiresInSec);
    }

    private static String normalizeUsername(final String username) {
        return (username == null) ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private static Role parseRequestedRole(final String requestedRole) {
        if (requestedRole == null || requestedRole.isBlank()) return Role.BUYER;
        try {
            final Role parsed = Role.valueOf(requestedRole.trim().toUpperCase(Locale.ROOT));
            return (parsed == Role.ADMIN) ? null : parsed;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public record RegisterRequest(
            @JsonAlias({"email"}) String username,
            String password,
            @JsonAlias({"role", "requestedRole"}) String requestedRole
    ) {}
    public record RegisterResponse(boolean ok, String verificationToken) {}
    public record VerifyEmailRequest(String token) {}
    public record LoginRequest(@JsonAlias({"email"}) String username, String password) {}
    public record RefreshRequest(String refreshToken) {}
    public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {}
    public record MfaChallengeResponse(boolean mfaRequired, String challengeId, String method, long expiresIn, String devCode) {}
}