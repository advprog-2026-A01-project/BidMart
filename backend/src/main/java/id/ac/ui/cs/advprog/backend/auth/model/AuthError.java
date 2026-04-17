package id.ac.ui.cs.advprog.backend.auth.model;

import org.springframework.http.HttpStatus;

public enum AuthError {
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "invalid_credentials"),
    INVALID_PRIVATE_KEY(HttpStatus.UNAUTHORIZED, "invalid_private_key"),
    PRIVATE_KEY_REQUIRED(HttpStatus.BAD_REQUEST, "private_key_required"),
    USERNAME_TAKEN(HttpStatus.CONFLICT, "username_taken"),
    USER_DISABLED(HttpStatus.FORBIDDEN, "user_disabled"),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "email_not_verified"),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "password_mismatch"),
    PASSWORD_TOO_SHORT(HttpStatus.BAD_REQUEST, "password_too_short"),
    IDENTITY_DOCUMENT_REQUIRED(HttpStatus.BAD_REQUEST, "identity_document_required"),
    IDENTITY_DOCUMENT_INVALID(HttpStatus.BAD_REQUEST, "identity_document_invalid"),
    IDENTITY_NAME_MISMATCH(HttpStatus.BAD_REQUEST, "identity_name_mismatch"),
    OCR_TEXT_MISSING(HttpStatus.BAD_REQUEST, "ocr_text_missing"),
    CAPTCHA_REQUIRED(HttpStatus.BAD_REQUEST, "captcha_required"),
    CAPTCHA_INVALID(HttpStatus.BAD_REQUEST, "captcha_invalid"),
    CAPTCHA_EXPIRED(HttpStatus.BAD_REQUEST, "captcha_expired"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "invalid_refresh_token"),
    TOO_MANY_SESSIONS(HttpStatus.TOO_MANY_REQUESTS, "too_many_sessions"),
    INVALID_MFA_CHALLENGE(HttpStatus.UNAUTHORIZED, "invalid_mfa_challenge"),
    INVALID_MFA_CODE(HttpStatus.UNAUTHORIZED, "invalid_mfa_code"),
    MFA_TOO_MANY_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, "mfa_too_many_attempts");

    private final HttpStatus httpStatus;
    private final String errorCode;

    AuthError(final HttpStatus httpStatus, final String errorCode) {
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public HttpStatus status() {
        return httpStatus;
    }

    public String code() {
        return errorCode;
    }
}