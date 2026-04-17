package id.ac.ui.cs.advprog.backend.auth.model;

import org.springframework.http.HttpStatus;

public class AuthException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;
    private final HttpStatus status;

    public AuthException(final HttpStatus status, final String code) {
        super(code);
        this.code = code;
        this.status = status;
    }

    public AuthException(final HttpStatus status, final String code, final Throwable cause) {
        super(code, cause);
        this.code = code;
        this.status = status;
    }

    public AuthException(final AuthError error) {
        this(error.status(), error.code());
    }

    public AuthException(final AuthError error, final Throwable cause) {
        this(error.status(), error.code(), cause);
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static AuthException of(final AuthError error) {
        return new AuthException(error);
    }
}