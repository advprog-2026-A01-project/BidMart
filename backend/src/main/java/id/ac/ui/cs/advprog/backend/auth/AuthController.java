package id.ac.ui.cs.advprog.backend.auth;

import com.fasterxml.jackson.annotation.JsonAlias;
import id.ac.ui.cs.advprog.backend.security.RequiresPermission;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@SuppressWarnings({
        "PMD.TooManyMethods"
})
public class AuthController {

    private static final String ERROR_KEY = "error";
    private static final String ERROR_UNAUTHORIZED = "unauthorized";

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
        final String password = body.password() == null ? "" : body.password();

        if (username.isBlank() || password.isBlank()) {
            return ResponseEntity.badRequest().body(err("invalid_input"));
        }

        final Role role = parseRequestedRole(body.requestedRole());
        if (role == null) {
            return ResponseEntity.badRequest().body(err("invalid_role"));
        }

        final var token = authService.register(username, password, role);
        return ResponseEntity.status(HttpStatus.CREATED).body(new RegisterResponse(true, token.toString()));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestBody final VerifyEmailRequest body) {
        final String token = body.token() == null ? "" : body.token().trim();
        if (token.isBlank()) return ResponseEntity.badRequest().body(err("invalid_input"));

        authService.verifyEmail(token);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody final LoginRequest body, final HttpServletRequest request) {
        final String username = normalizeUsername(body.username());
        final var meta = new AuthService.ClientMeta(request.getHeader("User-Agent"), request.getRemoteAddr());

        final var out = authService.login(username, body.password(), meta);

        if (out instanceof AuthService.LoginResult.Tokens t) {
            return ResponseEntity.ok(toTokenResponse(t.tokens()));
        }

        final AuthService.LoginResult.MfaRequired m = (AuthService.LoginResult.MfaRequired) out;
        return ResponseEntity.ok(new MfaChallengeResponse(
                true,
                m.challengeId().toString(),
                m.method(),
                m.expiresInSeconds(),
                m.devCode()
        ));
    }

    @PostMapping("/2fa/verify")
    public ResponseEntity<?> verifyMfa(@RequestBody final MfaVerifyRequest body, final HttpServletRequest request) {
        final var meta = new AuthService.ClientMeta(request.getHeader("User-Agent"), request.getRemoteAddr());
        final var tokens = authService.verifyMfa(body.challengeId(), body.code(), meta);
        return ResponseEntity.ok(toTokenResponse(tokens));
    }

    @PostMapping("/2fa/enable-email")
    @RequiresPermission("mfa:manage")
    public ResponseEntity<?> enableEmailMfa(final Authentication authentication) {
        final AuthPrincipal p = requirePrincipal(authentication);
        authService.enableEmailMfa(p.userId());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/2fa/disable")
    @RequiresPermission("mfa:manage")
    public ResponseEntity<?> disableMfa(final Authentication authentication) {
        final AuthPrincipal p = requirePrincipal(authentication);
        authService.disableMfa(p.userId());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    // ===== TOTP endpoints (NEW) =====

    @PostMapping("/2fa/totp/setup")
    @RequiresPermission("mfa:manage")
    public ResponseEntity<?> setupTotp(final Authentication authentication) {
        final AuthPrincipal p = requirePrincipal(authentication);
        final var setup = authService.setupTotp(p.userId(), p.username());
        return ResponseEntity.ok(new TotpSetupResponse(true, setup.secret(), setup.otpauthUrl()));
    }

    @PostMapping("/2fa/totp/enable")
    @RequiresPermission("mfa:manage")
    public ResponseEntity<?> enableTotp(final Authentication authentication, @RequestBody final TotpEnableRequest body) {
        final AuthPrincipal p = requirePrincipal(authentication);
        authService.enableTotp(p.userId(), body == null ? null : body.code());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/2fa/totp/disable")
    @RequiresPermission("mfa:manage")
    public ResponseEntity<?> disableTotp(final Authentication authentication) {
        final AuthPrincipal p = requirePrincipal(authentication);
        authService.disableTotp(p.userId());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody final RefreshRequest body, final HttpServletRequest request) {
        final var meta = new AuthService.ClientMeta(request.getHeader("User-Agent"), request.getRemoteAddr());
        final var tokenPair = authService.refresh(body.refreshToken(), meta);
        return ResponseEntity.ok(toTokenResponse(tokenPair));
    }

    @PostMapping("/logout")
    @RequiresPermission("session:logout")
    public ResponseEntity<?> logout(final Authentication authentication) {
        if (authentication == null || authentication.getDetails() == null) {
            return ResponseEntity.status(401).body(err(ERROR_UNAUTHORIZED));
        }
        final String accessToken = String.valueOf(authentication.getDetails());
        authService.logout(accessToken);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/me")
    @RequiresPermission("session:me")
    public ResponseEntity<?> me(final Authentication authentication) {
        final AuthPrincipal p = requirePrincipal(authentication);
        return ResponseEntity.ok(new MeResponse(p.username(), p.role()));
    }

    @GetMapping("/sessions")
    @RequiresPermission("session:list")
    public ResponseEntity<?> sessions(final Authentication authentication) {
        final AuthPrincipal p = requirePrincipal(authentication);
        final List<SessionRepository.SessionRow> sessions = sessionRepository.listSessions(p.userId());
        return ResponseEntity.ok(sessions);
    }

    @PostMapping("/sessions/{token}/revoke")
    @RequiresPermission("session:revoke")
    public ResponseEntity<?> revokeSession(@PathVariable("token") final String token, final Authentication authentication) {
        final AuthPrincipal p = requirePrincipal(authentication);

        final java.util.UUID t;
        try {
            t = java.util.UUID.fromString(token.trim());
        }
        catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(err("invalid_token"));
        }

        final int updated = sessionRepository.revokeByTokenAndUserId(t, p.userId(), java.time.Instant.now());
        if (updated == 0) return ResponseEntity.status(404).body(err("session_not_found"));
        return ResponseEntity.ok(Map.of("ok", true));
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
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private static Map<String, String> err(final String code) {
        return Map.of(ERROR_KEY, code);
    }

    private static Role parseRequestedRole(final String requestedRole) {
        if (requestedRole == null || requestedRole.isBlank()) return Role.BUYER;

        final String r = requestedRole.trim().toUpperCase(Locale.ROOT);
        try {
            final Role parsed = Role.valueOf(r);
            return (parsed == Role.ADMIN) ? null : parsed;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static AuthPrincipal requirePrincipal(final Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal p)) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, ERROR_UNAUTHORIZED);
        }
        return p;
    }

    public record RegisterRequest(
            @JsonAlias({"email"})
            String username,
            String password,
            @JsonAlias({"role", "requestedRole"})
            String requestedRole
    ) {}

    public record RegisterResponse(boolean ok, String verificationToken) {}
    public record VerifyEmailRequest(String token) {}

    public record LoginRequest(
            @JsonAlias({"email"})
            String username,
            String password
    ) {}

    public record MfaVerifyRequest(String challengeId, String code) {}
    public record MfaChallengeResponse(boolean mfaRequired, String challengeId, String method, long expiresIn, String devCode) {}

    public record TotpSetupResponse(boolean ok, String secret, String otpauthUrl) {}
    public record TotpEnableRequest(String code) {}

    public record RefreshRequest(String refreshToken) {}
    public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {}
    public record MeResponse(String username, String role) {}
}