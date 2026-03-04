package id.ac.ui.cs.advprog.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

public class PermissionInterceptor implements HandlerInterceptor {

    private static final String PERM_PREFIX = "PERM_";

    @Override
    public boolean preHandle(final HttpServletRequest req, final HttpServletResponse res, final Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod hm)) return true;

        RequiresPermission ann = hm.getMethodAnnotation(RequiresPermission.class);
        if (ann == null) ann = hm.getBeanType().getAnnotation(RequiresPermission.class);
        if (ann == null) return true;

        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            res.setStatus(401);
            res.setContentType("application/json");
            res.getWriter().write("{\"error\":\"unauthorized\"}");
            return false;
        }

        final String required = PERM_PREFIX + ann.value();
        final boolean ok = auth.getAuthorities().stream().anyMatch(a -> Objects.equals(a.getAuthority(), required));

        if (!ok) {
            res.setStatus(403);
            res.setContentType("application/json");
            res.getWriter().write("{\"error\":\"insufficient_permission\"}");
            return false;
        }

        return true;
    }
}