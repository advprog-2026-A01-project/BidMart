package id.ac.ui.cs.advprog.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import id.ac.ui.cs.advprog.backend.auth.model.Role;
import id.ac.ui.cs.advprog.backend.auth.repository.UserAuthRepository;

@Configuration
@ConditionalOnProperty(prefix = "admin.bootstrap", name = "enabled", havingValue = "true")
public class DevAdminSeedConfig {

    @Bean
    ApplicationRunner seedAdmin(
            UserAuthRepository userAuthRepository,
            PasswordEncoder passwordEncoder,
            @Value("${admin.bootstrap.username}") String username,
            @Value("${admin.bootstrap.password}") String password
    ) {
        return args -> {
            if (username == null || username.isBlank()) {
                throw new IllegalStateException("admin bootstrap enabled but username is blank");
            }
            if (password == null || password.isBlank()) {
                throw new IllegalStateException("admin bootstrap enabled but password is blank");
            }

            final String passwordHash = passwordEncoder.encode(password);
            var existing = userAuthRepository.findByUsername(username);

            if (existing.isEmpty()) {
                long id = userAuthRepository.insert(username, passwordHash, Role.ADMIN);
                userAuthRepository.setDisabled(id, false);
                userAuthRepository.setEmailVerified(id, true);
                return;
            }

            var user = existing.get();
            userAuthRepository.updateRoleName(user.id(), Role.ADMIN.name());
            userAuthRepository.updatePasswordHash(user.id(), passwordHash);
            userAuthRepository.setDisabled(user.id(), false);
            userAuthRepository.setEmailVerified(user.id(), true);
        };
    }
}