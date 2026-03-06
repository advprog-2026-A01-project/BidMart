package id.ac.ui.cs.advprog.backend.security;

import id.ac.ui.cs.advprog.backend.auth.model.AuthPrincipal;
import id.ac.ui.cs.advprog.backend.rbac.repository.RbacRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.stream.Collectors;

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
    private static final String DEFAULT_ROLE = "BUYER";
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
        if (auth == null) {
            filterChain.doFilter(request, response);
            return;
        }

        final AuthPrincipal principal = extractPrincipal(auth);
        if (principal == null || isAlreadyEnriched(auth)) {
            filterChain.doFilter(request, response);
            return;
        }

        final Authentication enriched = enrichAuthentication(auth, principal);
        SecurityContextHolder.getContext().setAuthentication(enriched);
        filterChain.doFilter(request, response);
    }

    private static AuthPrincipal extractPrincipal(final Authentication auth) {
        return (auth.getPrincipal() instanceof AuthPrincipal p) ? p : null;
    }

    private static boolean isAlreadyEnriched(final Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().startsWith(PERM_PREFIX));
    }

    private Authentication enrichAuthentication(final Authentication auth, final AuthPrincipal principal) {
        final String roleName = resolveRoleName(principal.role());
        final List<String> perms = rbacRepository.listPermissionsForRole(roleName);

        final List<GrantedAuthority> merged = new ArrayList<>(auth.getAuthorities());
        final Set<String> seen = merged.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toCollection(HashSet::new));

        for (String k : perms) {
            final String permAuthority = PERM_PREFIX + k;
            if (seen.add(permAuthority)) {
                merged.add(new SimpleGrantedAuthority(permAuthority));
            }
        }

        final var newAuth = new UsernamePasswordAuthenticationToken(auth.getPrincipal(), null, merged);
        newAuth.setDetails(auth.getDetails());
        return newAuth;
    }

    private static String resolveRoleName(final String raw) {
        if (raw == null || raw.isBlank()) return DEFAULT_ROLE;
        return raw.trim();
    }
}