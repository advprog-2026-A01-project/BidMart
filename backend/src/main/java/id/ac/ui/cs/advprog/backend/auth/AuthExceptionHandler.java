package id.ac.ui.cs.advprog.backend.auth;

import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex) {
        if ("invalid_credentials".equals(ex.getMessage())) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_credentials"));
        }
        if ("username_taken".equals(ex.getMessage())) {
            return ResponseEntity.status(409).body(Map.of("error", "username_taken"));
        }
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<?> handleDuplicateKey() {
        return ResponseEntity.status(409).body(Map.of("error", "username_taken"));
    }
}
