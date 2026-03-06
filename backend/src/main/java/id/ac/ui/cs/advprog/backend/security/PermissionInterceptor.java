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
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String ERROR_UNAUTHORIZED = "unauthorized";
    private static final String ERROR_INSUFFICIENT_PERMISSION = "insufficient_permission";

    @Override
    @SuppressWarnings("PMD.LawOfDemeter")
    public boolean preHandle(final HttpServletRequest req, final HttpServletResponse res, final Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod hm)) return true;

        RequiresPermission ann = hm.getMethodAnnotation(RequiresPermission.class);
        if (ann == null) ann = hm.getBeanType().getAnnotation(RequiresPermission.class);
        if (ann == null) return true;

        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            writeJsonError(res, 401, ERROR_UNAUTHORIZED);
            return false;
        }

        final String required = PERM_PREFIX + ann.value();
        final boolean ok = auth.getAuthorities().stream().anyMatch(a -> Objects.equals(a.getAuthority(), required));

        if (!ok) {
            writeJsonError(res, 403, ERROR_INSUFFICIENT_PERMISSION);
            return false;
        }

        return true;
    }

    @SuppressWarnings({"PMD.LawOfDemeter", "PMD.CloseResource"})
    private static void writeJsonError(final HttpServletResponse res, final int status, final String code) throws Exception {
        res.setStatus(status);
        res.setContentType(CONTENT_TYPE_JSON);
        try (var writer = res.getWriter()) {
            writer.write("{\"error\":\"" + code + "\"}");
            writer.flush();
        }
    }
}