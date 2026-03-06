package id.ac.ui.cs.advprog.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * Extracts access tokens from HTTP requests.

 * Supported:
 * - Authorization: Bearer <token>
 * - X-Auth-Token: <token> (legacy)
 */
public final class TokenExtractor {

    private TokenExtractor() {}

    public static Optional<String> extract(final HttpServletRequest request) {
        final String authz = request.getHeader("Authorization");
        if (authz != null && authz.startsWith("Bearer ")) {
            final String token = authz.substring("Bearer ".length()).trim();
            if (!token.isEmpty()) return Optional.of(token);
        }

        final String legacy = request.getHeader(TokenAuthFilter.HEADER);
        if (legacy != null && !legacy.isBlank()) {
            return Optional.of(legacy.trim());
        }

        return Optional.empty();
    }
}