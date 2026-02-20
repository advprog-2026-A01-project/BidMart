package id.ac.ui.cs.advprog.backend.auth;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, SessionRepository sessionRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void register(String username, String password) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("username_taken");
        }
        String hash = passwordEncoder.encode(password);
        userRepository.insert(username, hash);
    }

    public UUID login(String username, String password) {
        var user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("invalid_credentials"));

        if (!passwordEncoder.matches(password, user.passwordHash())) {
            throw new IllegalArgumentException("invalid_credentials");
        }

        return sessionRepository.create(user.id());
    }
}
