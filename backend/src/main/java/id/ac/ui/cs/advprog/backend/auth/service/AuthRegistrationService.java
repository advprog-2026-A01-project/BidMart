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
import org.springframework.web.multipart.MultipartFile;

@Service
public class AuthRegistrationService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthRegistrationService.class);
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserAuthRepository userAuthRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationRepository emailVerificationRepository;
    private final AuthProperties props;
    private final Clock clock;
    private final EmailService emailService;
    private final IdentityDocumentValidationService identityDocumentValidationService;
    private final PersonalKeyService personalKeyService;

    public AuthRegistrationService(
            final UserAuthRepository userAuthRepository,
            final PasswordEncoder passwordEncoder,
            final EmailVerificationRepository emailVerificationRepository,
            final AuthProperties props,
            final Clock clock,
            final EmailService emailService,
            final IdentityDocumentValidationService identityDocumentValidationService,
            final PersonalKeyService personalKeyService
    ) {
        this.userAuthRepository = userAuthRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationRepository = emailVerificationRepository;
        this.props = props;
        this.clock = clock;
        this.emailService = emailService;
        this.identityDocumentValidationService = identityDocumentValidationService;
        this.personalKeyService = personalKeyService;
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
    public IdentityRegistrationResult registerWithIdentity(
            final String username,
            final String password,
            final String confirmPassword,
            final String legalName,
            final Role role,
            final String documentType,
            final String documentExtractedText,
            final MultipartFile documentImage
    ) {
        final String safeUsername = normalizeUsername(username);
        validateNewIdentityRegistration(safeUsername, password, confirmPassword, legalName, role);

        if (userAuthRepository.findByUsername(safeUsername).isPresent()) {
            throw AuthException.usernameTaken();
        }

        final Role safeRole = (role == null) ? Role.BUYER : role;
        final var verifiedDocument = identityDocumentValidationService.validate(
                legalName,
                documentType,
                documentExtractedText,
                documentImage
        );

        final String passwordHash = passwordEncoder.encode(password);
        final String rawKey = personalKeyService.generateRawKey();
        final String personalKeyHash = passwordEncoder.encode(rawKey);
        final Instant now = Instant.now(clock);

        userAuthRepository.insertVerifiedIdentityUser(
                safeUsername,
                passwordHash,
                safeRole,
                legalName,
                personalKeyHash,
                verifiedDocument.documentType(),
                verifiedDocument.normalizedOcrText(),
                now
        );

        final String issuedAtIso = now.toString();
        return new IdentityRegistrationResult(
                true,
                null,
                rawKey,
                personalKeyService.buildDownloadFilename(safeUsername),
                personalKeyService.buildDownloadContents(safeUsername, legalName, safeRole, rawKey, issuedAtIso),
                issuedAtIso,
                safeUsername,
                safeRole.name(),
                legalName
        );
    }

    @Transactional
    public RotatedPersonalKeyResult rotatePersonalKey(final long userId) {
        final var user = userAuthRepository.findById(userId).orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "user_not_found"));
        final String rawKey = personalKeyService.generateRawKey();
        final String hashed = passwordEncoder.encode(rawKey);
        final Instant now = Instant.now(clock);
        userAuthRepository.updatePersonalKey(userId, hashed, now);

        final String issuedAtIso = now.toString();
        return new RotatedPersonalKeyResult(
                true,
                rawKey,
                personalKeyService.buildDownloadFilename(user.username()),
                personalKeyService.buildDownloadContents(user.username(), user.legalName(), user.role(), rawKey, issuedAtIso),
                issuedAtIso
        );
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

    private void validateNewIdentityRegistration(
            final String username,
            final String password,
            final String confirmPassword,
            final String legalName,
            final Role role
    ) {
        if (username.isBlank() || safe(password).isBlank() || safe(confirmPassword).isBlank() || safe(legalName).isBlank()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "invalid_input");
        }
        if ((role == null ? Role.BUYER : role) == Role.ADMIN) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "invalid_role");
        }
        if (!safe(password).equals(confirmPassword)) {
            throw AuthException.passwordMismatch();
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw AuthException.passwordTooShort();
        }
    }

    private static String normalizeUsername(final String username) {
        return (username == null) ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private static String safe(final String value) {
        return (value == null) ? "" : value;
    }

    public record IdentityRegistrationResult(
            boolean ok,
            String verificationToken,
            String privateKey,
            String downloadFilename,
            String downloadContent,
            String issuedAt,
            String username,
            String role,
            String legalName
    ) {}

    public record RotatedPersonalKeyResult(
            boolean ok,
            String privateKey,
            String downloadFilename,
            String downloadContent,
            String issuedAt
    ) {}

    static final class UuidParser {
        private UuidParser() {}
        static Optional<UUID> parseOptional(final String raw) {
            if (raw == null || raw.isBlank()) return Optional.empty();
            try { return Optional.of(UUID.fromString(raw.trim())); }
            catch (IllegalArgumentException ex) { return Optional.empty(); }
        }
    }
}