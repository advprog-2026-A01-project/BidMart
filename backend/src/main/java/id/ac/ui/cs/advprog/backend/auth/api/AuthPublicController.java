package id.ac.ui.cs.advprog.backend.auth.api;

import com.fasterxml.jackson.annotation.JsonAlias;
import id.ac.ui.cs.advprog.backend.auth.model.AuthProperties;
import id.ac.ui.cs.advprog.backend.auth.model.Role;
import id.ac.ui.cs.advprog.backend.auth.repository.SessionRepository;
import id.ac.ui.cs.advprog.backend.auth.service.AuthLoginService;
import id.ac.ui.cs.advprog.backend.auth.service.AuthRegistrationService;
import id.ac.ui.cs.advprog.backend.auth.service.AuthTokenService;
import id.ac.ui.cs.advprog.backend.auth.service.CaptchaService;
import id.ac.ui.cs.advprog.backend.auth.service.IdentityRegistrationCommand;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
public class AuthPublicController {

    private static final String ERROR_KEY = "error";

    private final AuthRegistrationService registrationService;
    private final AuthLoginService loginService;
    private final AuthTokenService tokenService;
    private final AuthProperties props;
    private final CaptchaService captchaService;

    public AuthPublicController(
            final AuthRegistrationService registrationService,
            final AuthLoginService loginService,
            final AuthTokenService tokenService,
            final AuthProperties props,
            final CaptchaService captchaService
    ) {
        this.registrationService = registrationService;
        this.loginService = loginService;
        this.tokenService = tokenService;
        this.props = props;
        this.captchaService = captchaService;
    }

    @GetMapping("/captcha")
    public ResponseEntity<?> issueCaptcha() {
        return ResponseEntity.ok(captchaService.issueChallenge());
    }

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> register(@RequestBody final RegisterRequest body) {
        final String username = normalizeUsername(body.username());
        final String password = (body.password() == null) ? "" : body.password();

        if (username.isBlank() || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "invalid_input"));
        }

        final Role role = parseRequestedRole(body.requestedRole());
        if (role == null) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "invalid_role"));
        }

        final var token = registrationService.register(username, password, role);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegisterResponse(true, token.toString(), null, null, null, null, null, null, null));
    }

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> registerWithIdentity(@ModelAttribute final IdentityRegisterForm form) {
        final Role role = form.parsedRequestedRole();
        if (role == null) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "invalid_role"));
        }

        final var result = registrationService.registerWithIdentity(form.toCommand(role));

        return ResponseEntity.status(HttpStatus.CREATED).body(new RegisterResponse(
                result.ok(),
                result.verificationToken(),
                result.privateKey(),
                result.downloadFilename(),
                result.downloadContent(),
                result.issuedAt(),
                result.username(),
                result.role(),
                result.legalName()
        ));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestBody final VerifyEmailRequest body) {
        final String token = (body.token() == null) ? "" : body.token().trim();
        if (token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "invalid_input"));
        }
        registrationService.verifyEmail(normalizeUsername(body.username()), token);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody final LoginRequest body, final HttpServletRequest request) {
        final String username = normalizeUsername(body.username());
        final var meta = new AuthLoginService.ClientMeta(request.getHeader("User-Agent"), request.getRemoteAddr());

        if (body.privateKey() == null || body.privateKey().isBlank()) {
            captchaService.verify(body.captchaId(), body.captchaAnswer());
        }

        final var out = loginService.login(username, body.password(), body.privateKey(), meta);
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
        if (requestedRole == null || requestedRole.isBlank()) {
            return Role.BUYER;
        }
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

    public record RegisterResponse(
            boolean ok,
            String verificationToken,
            String privateKey,
            String downloadFilename,
            String downloadContent,
            String issuedAt,
            String username,
            String role,
            String legalName
    ) {}

    public record VerifyEmailRequest(String token, @JsonAlias({"email"}) String username) {}

    public record LoginRequest(
            @JsonAlias({"email"}) String username,
            String password,
            @JsonAlias({"otp", "privateKey", "private_key"}) String privateKey,
            String captchaId,
            String captchaAnswer
    ) {}

    public record RefreshRequest(String refreshToken) {}
    public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {}
    public record MfaChallengeResponse(boolean mfaRequired, String challengeId, String method, long expiresIn, String devCode) {}

    public static class IdentityRegisterForm {
        private String username;
        private String password;
        private String confirmPassword;
        private String legalName;
        private String requestedRole;
        private String documentType;
        private String documentExtractedText;
        private MultipartFile documentImage;

        public Role parsedRequestedRole() {
            return parseRequestedRole(requestedRole);
        }

        public IdentityRegistrationCommand toCommand(final Role role) {
            return new IdentityRegistrationCommand(
                    normalizeUsername(username),
                    password,
                    confirmPassword,
                    legalName,
                    role,
                    documentType,
                    documentExtractedText,
                    documentImage
            );
        }

        public boolean hasIdentityDocument() {
            return documentImage != null && !documentImage.isEmpty();
        }

        public boolean hasPasswordConfirmation() {
            return confirmPassword != null && !confirmPassword.isBlank();
        }

        public boolean hasRequestedRole() {
            return requestedRole != null && !requestedRole.isBlank();
        }

        public boolean hasLegalName() {
            return legalName != null && !legalName.isBlank();
        }

        public void setUsername(final String username) {
            this.username = username;
        }

        public void setPassword(final String password) {
            this.password = password;
        }

        public void setConfirmPassword(final String confirmPassword) {
            this.confirmPassword = confirmPassword;
        }

        public void setLegalName(final String legalName) {
            this.legalName = legalName;
        }

        public void setRequestedRole(final String requestedRole) {
            this.requestedRole = requestedRole;
        }

        public void setDocumentType(final String documentType) {
            this.documentType = documentType;
        }

        public void setDocumentExtractedText(final String documentExtractedText) {
            this.documentExtractedText = documentExtractedText;
        }

        public void setDocumentImage(final MultipartFile documentImage) {
            this.documentImage = documentImage;
        }
    }
}