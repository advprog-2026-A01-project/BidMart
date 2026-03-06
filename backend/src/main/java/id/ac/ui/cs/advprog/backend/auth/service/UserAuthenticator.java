package id.ac.ui.cs.advprog.backend.auth.service;

import id.ac.ui.cs.advprog.backend.auth.model.AuthException;
import id.ac.ui.cs.advprog.backend.auth.repository.UserAuthRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserAuthenticator {

    private final UserAuthRepository userAuthRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAuthenticator(final UserAuthRepository userAuthRepository, final PasswordEncoder passwordEncoder) {
        this.userAuthRepository = userAuthRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserAuthRepository.UserRow authenticate(final String username, final String password) {
        final var user = userAuthRepository.findByUsername(username).orElseThrow(AuthException::invalidCredentials);

        if (user.disabled()) throw AuthException.userDisabled();
        if (!user.emailVerified()) throw AuthException.emailNotVerified();

        final String safePassword = (password == null) ? "" : password;
        if (!passwordEncoder.matches(safePassword, user.passwordHash())) {
            throw AuthException.invalidCredentials();
        }

        return user;
    }
}