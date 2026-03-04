package id.ac.ui.cs.advprog.backend.auth;

import org.springframework.http.HttpStatus;

/*
Tanggung jawab: error domain auth yang konsisten (code + status).
 */
public class AuthException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;
    private final HttpStatus status;

    public AuthException(final HttpStatus status, final String code) {
        super(code);
        this.code = code;
        this.status = status;
    }

    public String getCode() { return code; }
    public HttpStatus getStatus() { return status; }

    public static AuthException invalidCredentials() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "invalid_credentials");
    }

    public static AuthException usernameTaken() {
        return new AuthException(HttpStatus.CONFLICT, "username_taken");
    }

    public static AuthException userDisabled() {
        return new AuthException(HttpStatus.FORBIDDEN, "user_disabled");
    }

    public static AuthException emailNotVerified() {
        return new AuthException(HttpStatus.FORBIDDEN, "email_not_verified");
    }

    public static AuthException refreshTokenInvalid() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "invalid_refresh_token");
    }

    public static AuthException tooManySessions() {
        return new AuthException(HttpStatus.TOO_MANY_REQUESTS, "too_many_sessions");
    }

    public static AuthException invalidMfaChallenge() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "invalid_mfa_challenge");
    }

    public static AuthException invalidMfaCode() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "invalid_mfa_code");
    }

    public static AuthException mfaTooManyAttempts() {
        return new AuthException(HttpStatus.TOO_MANY_REQUESTS, "mfa_too_many_attempts");
    }
}