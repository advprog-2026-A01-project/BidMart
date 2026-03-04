package id.ac.ui.cs.advprog.backend.auth;

import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/*
Tanggung jawab: mapping exception → response JSON stabil.
 */
@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<?> handleAuthException(final AuthException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of("error", ex.getCode()));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<?> handleDuplicateKey() {
        // keep error code stable for frontend
        return ResponseEntity.status(409).body(Map.of("error", "username_taken"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(final IllegalArgumentException ex) {
        // generic fallback (input validation, etc.)
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}