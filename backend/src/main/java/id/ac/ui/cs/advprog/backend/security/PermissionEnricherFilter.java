package id.ac.ui.cs.advprog.backend.security;

import id.ac.ui.cs.advprog.backend.auth.AuthPrincipal;
import id.ac.ui.cs.advprog.backend.auth.RbacRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class PermissionEnricherFilter extends OncePerRequestFilter {

    private static final String PERM_PREFIX = "PERM_";
    private final RbacRepository rbacRepository;

    public PermissionEnricherFilter(final RbacRepository rbacRepository) {
        this.rbacRepository = rbacRepository;
    }

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws ServletException, IOException {

        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getPrincipal() instanceof AuthPrincipal p) {
            // avoid re-enriching
            final boolean already = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().startsWith(PERM_PREFIX));
            if (!already) {
                final String roleName = (p.role() == null || p.role().isBlank()) ? "BUYER" : p.role();

                final List<String> perms = rbacRepository.listPermissionsForRole(roleName);

                // IMPORTANT: use explicit type to avoid CAP#1 wildcard issues
                final List<GrantedAuthority> newAuths = new ArrayList<>();
                newAuths.addAll(auth.getAuthorities());

                final Set<String> seen = new HashSet<>();
                for (GrantedAuthority a : newAuths) {
                    seen.add(a.getAuthority());
                }

                for (String k : perms) {
                    final String permAuthority = PERM_PREFIX + k;
                    if (seen.add(permAuthority)) {
                        newAuths.add(new SimpleGrantedAuthority(permAuthority));
                    }
                }

                final var newAuth = new UsernamePasswordAuthenticationToken(auth.getPrincipal(), null, newAuths);
                newAuth.setDetails(auth.getDetails());
                SecurityContextHolder.getContext().setAuthentication(newAuth);
            }
        }

        filterChain.doFilter(request, response);
    }
}