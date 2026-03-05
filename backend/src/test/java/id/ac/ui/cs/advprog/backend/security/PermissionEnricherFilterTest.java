package id.ac.ui.cs.advprog.backend.security;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import id.ac.ui.cs.advprog.backend.auth.AuthPrincipal;
import id.ac.ui.cs.advprog.backend.auth.RbacRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({
        "PMD.UnitTestContainsTooManyAsserts",
        "PMD.UnitTestAssertionsShouldIncludeMessage"
})
class PermissionEnricherFilterTest {

    private static final String ROLE_BUYER = "BUYER";
    private static final String AUTH_ROLE_BUYER = "ROLE_" + ROLE_BUYER;
    private static final String PERM_PREFIX = "PERM_";
    private static final String PERM_BID_PLACE = "bid:place";
    private static final String PERM_AUCTION_CREATE = "auction:create";

    @Mock private RbacRepository rbacRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void adds_permissions_when_missing() throws Exception {
        when(rbacRepository.listPermissionsForRole(ROLE_BUYER)).thenReturn(List.of(PERM_BID_PLACE, PERM_AUCTION_CREATE));

        final var principal = new AuthPrincipal(1L, "u", ROLE_BUYER);
        final var baseAuth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority(AUTH_ROLE_BUYER))
        );
        SecurityContextHolder.getContext().setAuthentication(baseAuth);

        final var filter = new PermissionEnricherFilter(rbacRepository);
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());

        final var enriched = SecurityContextHolder.getContext().getAuthentication();
        final Set<String> authorities = enriched.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toSet());

        assertTrue(authorities.contains(AUTH_ROLE_BUYER));
        assertTrue(authorities.contains(PERM_PREFIX + PERM_BID_PLACE));
        assertTrue(authorities.contains(PERM_PREFIX + PERM_AUCTION_CREATE));
        verify(rbacRepository).listPermissionsForRole(ROLE_BUYER);
    }

    @Test
    void does_not_reenrich_when_already_enriched() throws Exception {
        final var principal = new AuthPrincipal(1L, "u", ROLE_BUYER);
        final var already = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(
                        new SimpleGrantedAuthority(AUTH_ROLE_BUYER),
                        new SimpleGrantedAuthority(PERM_PREFIX + PERM_BID_PLACE)
                )
        );
        SecurityContextHolder.getContext().setAuthentication(already);

        final var filter = new PermissionEnricherFilter(rbacRepository);
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());

        verify(rbacRepository, never()).listPermissionsForRole(ROLE_BUYER);
    }
}