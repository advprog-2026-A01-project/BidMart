package id.ac.ui.cs.advprog.backend.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
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

// Kebutuhan mailing
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@Service
public class AuthRegistrationService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthRegistrationService.class);

    private final UserAuthRepository userAuthRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationRepository emailVerificationRepository;
    private final JavaMailSender mailSender;
    private final AuthProperties props;
    private final Clock clock;

    public AuthRegistrationService(
            final UserAuthRepository userAuthRepository,
            final PasswordEncoder passwordEncoder,
            final EmailVerificationRepository emailVerificationRepository,
            final AuthProperties props,
            final Clock clock,
            final JavaMailSender mailSender
    ) {
        this.userAuthRepository = userAuthRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationRepository = emailVerificationRepository;
        this.props = props;
        this.clock = clock;
        this.mailSender = mailSender;
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
        final UUID token = emailVerificationRepository.createToken(userId, now, now.plus(ttl));

        try {
            final SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(username);
            msg.setSubject("BidMart Verification");
            msg.setText("Verification token: " + token);
            mailSender.send(msg);
        } catch (MailException ex) {
            log.warn("Failed to send verification email to {}", username, ex);
        }

        log.info("DEV EMAIL VERIFY token for {}: {}", username, token);
        return token;
    }

    @Transactional
    public void verifyEmail(final String tokenString) {
        final UUID token = UuidParser.parseBadRequest(tokenString, "invalid_token");
        final Instant now = Instant.now(clock);

        final long userId = emailVerificationRepository.consumeIfValid(token, now)
                .orElseThrow(() -> new AuthException(HttpStatus.BAD_REQUEST, "invalid_or_expired_token"));

        userAuthRepository.setEmailVerified(userId, true);
    }

    static final class UuidParser {
        private UuidParser() {}
        static UUID parseBadRequest(final String raw, final String code) {
            if (raw == null || raw.isBlank()) throw new AuthException(HttpStatus.BAD_REQUEST, code);
            try { return UUID.fromString(raw.trim()); }
            catch (IllegalArgumentException ex) { throw new AuthException(HttpStatus.BAD_REQUEST, code, ex); }
        }
    }
}