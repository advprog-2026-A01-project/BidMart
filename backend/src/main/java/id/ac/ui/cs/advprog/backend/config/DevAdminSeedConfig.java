package id.ac.ui.cs.advprog.backend.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import id.ac.ui.cs.advprog.backend.auth.model.Role;
import id.ac.ui.cs.advprog.backend.auth.repository.UserAuthRepository;

// Ini untuk seeding admin.
@Configuration
@Profile("dev")
public class DevAdminSeedConfig {

    @Bean
    ApplicationRunner seedAdmin(UserAuthRepository userAuthRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            final String username = "admin";
            final String password = "admin123";

            var existing = userAuthRepository.findByUsername(username);
            if (existing.isEmpty()) {
                userAuthRepository.insert(username, passwordEncoder.encode(password), Role.ADMIN);
                return;
            }

            var user = existing.get();
            userAuthRepository.updateRoleName(user.id(), Role.ADMIN.name());
            userAuthRepository.setDisabled(user.id(), false);
            userAuthRepository.setEmailVerified(user.id(), true);
        };
    }
}