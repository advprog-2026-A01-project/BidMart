package id.ac.ui.cs.advprog.backend.security;

import id.ac.ui.cs.advprog.backend.auth.AuthPrincipal;
import id.ac.ui.cs.advprog.backend.auth.SessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/*
Tanggung jawab: middleware stateless:

- validasi token di DB
- set Authentication + ROLE_*
- simpan token ke auth.setDetails(token) untuk kebutuhan logout

 */
@Component
public class TokenAuthFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Auth-Token";
    private final SessionRepository sessionRepository;

    public TokenAuthFilter(final SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            TokenExtractor.extract(request).ifPresent(token -> {
                sessionRepository.findActiveByAccessToken(token, Instant.now()).ifPresent(session -> {
                    final String roleName = (session.role() == null || session.role().isBlank()) ? "BUYER" : session.role();
                    final var principal = new AuthPrincipal(session.userId(), session.username(), roleName);

                    final List<SimpleGrantedAuthority> authorities = List.of(
                            new SimpleGrantedAuthority("ROLE_" + roleName)
                    );

                    final var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    auth.setDetails(token);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                });
            });
        }

        filterChain.doFilter(request, response);
    }
}