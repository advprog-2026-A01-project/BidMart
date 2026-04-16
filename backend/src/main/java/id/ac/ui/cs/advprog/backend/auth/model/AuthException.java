package id.ac.ui.cs.advprog.backend.auth.model;

import org.springframework.http.HttpStatus;

/*
Tanggung jawab: error domain auth yang konsisten (code + status). Buatan sendiri
 */
public class AuthException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private final String code;
    private final HttpStatus status;

    // Constructor
    public AuthException(final HttpStatus status, final String code) {
        super(code);
        this.code = code;
        this.status = status;
    }

    // Constructor
    public AuthException(final HttpStatus status, final String code, final Throwable cause) {
        super(code, cause);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static AuthException invalidCredentials() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "invalid_credentials");
    }

    // -- Untuk Generate RandomKey for user upon register and login --
    public static AuthException invalidPrivateKey() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "invalid_private_key");
    }

    public static AuthException privateKeyRequired() {
        return new AuthException(HttpStatus.BAD_REQUEST, "private_key_required");
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

    // -- Untuk Generate AuthException username, password, and other kinds of mismatch for user upon register and login --
    public static AuthException passwordMismatch() {
        return new AuthException(HttpStatus.BAD_REQUEST, "password_mismatch");
    }

    public static AuthException passwordTooShort() {
        return new AuthException(HttpStatus.BAD_REQUEST, "password_too_short");
    }

    public static AuthException identityDocumentRequired() {
        return new AuthException(HttpStatus.BAD_REQUEST, "identity_document_required");
    }

    public static AuthException identityDocumentInvalid() {
        return new AuthException(HttpStatus.BAD_REQUEST, "identity_document_invalid");
    }

    public static AuthException identityNameMismatch() {
        return new AuthException(HttpStatus.BAD_REQUEST, "identity_name_mismatch");
    }

    public static AuthException ocrTextMissing() {
        return new AuthException(HttpStatus.BAD_REQUEST, "ocr_text_missing");
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