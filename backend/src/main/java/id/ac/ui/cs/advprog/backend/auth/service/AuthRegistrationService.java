package id.ac.ui.cs.advprog.backend.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.ac.ui.cs.advprog.backend.auth.model.AuthException;
import id.ac.ui.cs.advprog.backend.auth.model.AuthProperties;
import id.ac.ui.cs.advprog.backend.auth.model.Role;
import id.ac.ui.cs.advprog.backend.auth.repository.EmailVerificationRepository;
import id.ac.ui.cs.advprog.backend.auth.repository.UserAuthRepository;

@Service
public class AuthRegistrationService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthRegistrationService.class);

    private final UserAuthRepository userAuthRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationRepository emailVerificationRepository;
    private final AuthProperties props;
    private final Clock clock;
    private final EmailService emailService;

    public AuthRegistrationService(
            final UserAuthRepository userAuthRepository,
            final PasswordEncoder passwordEncoder,
            final EmailVerificationRepository emailVerificationRepository,
            final AuthProperties props,
            final Clock clock,
            final EmailService emailService
    ) {
        this.userAuthRepository = userAuthRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationRepository = emailVerificationRepository;
        this.props = props;
        this.clock = clock;
        this.emailService = emailService;
    }

    @Transactional
    public UUID register(final String username, final String password, final Role role) {
        if (userAuthRepository.findByUsername(username).isPresent()) {
            throw AuthException.usernameTaken();
        }

        final Role safeRole = (role == null) ? Role.BUYER : role;
        if (safeRole == Role.ADMIN) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "invalid_role");
        }

        final String hash = passwordEncoder.encode(password == null ? "" : password);
        final long userId = userAuthRepository.insert(username, hash, safeRole);

        final Instant now = Instant.now(clock);
        final Duration ttl = Duration.ofMinutes(props.getEmailVerifyTtlMinutes());
        final String demoCode = shouldUseDemoCode(safeRole) ? props.getDemoEmailVerificationCode() : null;
        final UUID token = emailVerificationRepository.createToken(userId, now, now.plus(ttl), demoCode);

        if (demoCode != null) {
            log.info("DEV EMAIL VERIFY code for {}: {}", username, demoCode);
            emailService.sendVerificationToken(username, demoCode);
        } else {
            log.info("DEV EMAIL VERIFY token for {}: {}", username, token);
            emailService.sendVerificationToken(username, token.toString());
        }
        return token;
    }

    @Transactional
    public void verifyEmail(final String username, final String tokenString) {
        final Instant now = Instant.now(clock);
        final Optional<UUID> token = UuidParser.parseOptional(tokenString);

        if (token.isPresent()) {
            final long userId = emailVerificationRepository.consumeIfValid(token.get(), now)
                    .orElseThrow(() -> new AuthException(HttpStatus.BAD_REQUEST, "invalid_or_expired_token"));
            userAuthRepository.setEmailVerified(userId, true);
            return;
        }

        final String safeUsername = normalizeUsername(username);
        final long userId = emailVerificationRepository.consumeByUsernameAndCodeIfValid(safeUsername, tokenString, now)
                .orElseThrow(() -> new AuthException(HttpStatus.BAD_REQUEST, "invalid_or_expired_token"));

        userAuthRepository.setEmailVerified(userId, true);
    }

    private boolean shouldUseDemoCode(final Role role) {
        return props.isDemoStaticCodesEnabled() && role != Role.ADMIN;
    }

    private static String normalizeUsername(final String username) {
        return (username == null) ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    static final class UuidParser {
        private UuidParser() {}
        static Optional<UUID> parseOptional(final String raw) {
            if (raw == null || raw.isBlank()) return Optional.empty();
            try { return Optional.of(UUID.fromString(raw.trim())); }
            catch (IllegalArgumentException ex) { return Optional.empty(); }
        }
    }
}