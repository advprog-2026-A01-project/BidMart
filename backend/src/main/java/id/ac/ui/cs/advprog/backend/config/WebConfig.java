package id.ac.ui.cs.advprog.backend.config;

import id.ac.ui.cs.advprog.backend.security.PermissionInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

// Untuk kebutuhan global
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(new PermissionInterceptor());
    }
}