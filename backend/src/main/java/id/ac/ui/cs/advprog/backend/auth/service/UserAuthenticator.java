package id.ac.ui.cs.advprog.backend.auth.service;

import id.ac.ui.cs.advprog.backend.auth.model.AuthError;
import id.ac.ui.cs.advprog.backend.auth.model.AuthException;
import id.ac.ui.cs.advprog.backend.auth.model.Role;
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
        final var user = userAuthRepository.findByUsername(username)
                .orElseThrow(() -> AuthException.of(AuthError.INVALID_CREDENTIALS));

        if (user.disabled()) {
            throw AuthException.of(AuthError.USER_DISABLED);
        }

        if (!user.emailVerified()) {
            throw AuthException.of(AuthError.EMAIL_NOT_VERIFIED);
        }

        final String safePassword = (password == null) ? "" : password;
        if (!passwordEncoder.matches(safePassword, user.passwordHash())) {
            throw AuthException.of(AuthError.INVALID_CREDENTIALS);
        }

        return user;
    }

    public void verifyPersonalKey(final UserAuthRepository.UserRow user, final String privateKey) {
        final UserAuthRepository.UserRow safeUser = requireUser(user);

        if (safeUser.role() == Role.ADMIN) {
            return;
        }

        final String storedHash = requireStoredPersonalKey(safeUser);
        final String providedKey = requireProvidedPersonalKey(privateKey);

        if (!passwordEncoder.matches(providedKey, storedHash)) {
            throw AuthException.of(AuthError.INVALID_PRIVATE_KEY);
        }
    }

    private UserAuthRepository.UserRow requireUser(final UserAuthRepository.UserRow user) {
        if (user == null) {
            throw AuthException.of(AuthError.INVALID_CREDENTIALS);
        }
        return user;
    }

    private String requireStoredPersonalKey(final UserAuthRepository.UserRow user) {
        final String storedHash = user.personalKeyHash();
        if (storedHash == null || storedHash.isBlank()) {
            throw AuthException.of(AuthError.PRIVATE_KEY_REQUIRED);
        }
        return storedHash;
    }

    private String requireProvidedPersonalKey(final String privateKey) {
        final String safePrivateKey = (privateKey == null) ? "" : privateKey.trim();
        if (safePrivateKey.isBlank()) {
            throw AuthException.of(AuthError.PRIVATE_KEY_REQUIRED);
        }
        return safePrivateKey;
    }
}