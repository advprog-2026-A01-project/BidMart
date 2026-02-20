package id.ac.ui.cs.advprog.backend.security;

import id.ac.ui.cs.advprog.backend.auth.SessionRepository;
import java.io.IOException;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class TokenAuthFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Auth-Token";

    private final SessionRepository sessionRepository;

    public TokenAuthFilter(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String token = request.getHeader(HEADER);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Optional<String> username = sessionRepository.findUsernameByToken(token);
            if (username.isPresent()) {
                var auth = new UsernamePasswordAuthenticationToken(username.get(), null, java.util.List.of());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }
}
