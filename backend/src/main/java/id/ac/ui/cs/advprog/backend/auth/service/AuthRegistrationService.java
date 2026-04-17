package id.ac.ui.cs.advprog.backend.auth.service;

import id.ac.ui.cs.advprog.backend.auth.model.AuthError;
import id.ac.ui.cs.advprog.backend.auth.model.AuthException;
import id.ac.ui.cs.advprog.backend.auth.model.AuthProperties;
import id.ac.ui.cs.advprog.backend.auth.model.Role;
import id.ac.ui.cs.advprog.backend.auth.repository.EmailVerificationRepository;
import id.ac.ui.cs.advprog.backend.auth.repository.UserAuthRepository;
import id.ac.ui.cs.advprog.backend.auth.repository.UserSecurityRepository;
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

@Service
public class AuthRegistrationService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthRegistrationService.class);
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final String ERROR_INVALID_INPUT = "invalid_input";

    private final UserAuthRepository userAuthRepository;
    private final UserSecurityRepository userSecurityRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationRepository emailVerificationRepository;
    private final AuthProperties props;
    private final Clock clock;
    private final EmailService emailService;
    private final IdentityDocumentValidationService identityDocumentValidationService;
    private final PersonalKeyService personalKeyService;

    public AuthRegistrationService(
            final UserAuthRepository userAuthRepository,
            final UserSecurityRepository userSecurityRepository,
            final PasswordEncoder passwordEncoder,
            final EmailVerificationRepository emailVerificationRepository,
            final AuthProperties props,
            final Clock clock,
            final EmailService emailService,
            final IdentityDocumentValidationService identityDocumentValidationService,
            final PersonalKeyService personalKeyService
    ) {
        this.userAuthRepository = userAuthRepository;
        this.userSecurityRepository = userSecurityRepository;
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
            throw AuthException.of(AuthError.USERNAME_TAKEN);
        }

        final Role safeRole = (role == null) ? Role.BUYER : role;
        if (safeRole == Role.ADMIN) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "invalid_role");
        }

        final String hash = passwordEncoder.encode(password == null ? "" : password);
        final long userId = userAuthRepository.insert(username, hash, safeRole);

        final Instant now = Instant.now(clock);
        final Duration ttl = Duration.ofMinutes(props.getEmailVerifyTtlMinutes());
        final String demoCode = RegistrationSupport.shouldUseDemoCode(props, safeRole)
                ? props.getDemoEmailVerificationCode()
                : null;
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
    public IdentityRegistrationResult registerWithIdentity(final IdentityRegistrationCommand command) {
        final IdentityRegistrationCommand normalizedCommand =
                RegistrationSupport.normalizeRegistrationCommand(command);
        RegistrationSupport.validateNewIdentityRegistration(normalizedCommand, MIN_PASSWORD_LENGTH);

        if (userAuthRepository.findByUsername(normalizedCommand.username()).isPresent()) {
            throw AuthException.of(AuthError.USERNAME_TAKEN);
        }

        final var verifiedDocument = identityDocumentValidationService.validate(
                normalizedCommand.legalName(),
                normalizedCommand.documentType(),
                normalizedCommand.documentExtractedText(),
                normalizedCommand.documentImage()
        );

        final String passwordHash = passwordEncoder.encode(normalizedCommand.password());
        final String rawKey = personalKeyService.generateRawKey();
        final String personalKeyHash = passwordEncoder.encode(rawKey);
        final Instant now = Instant.now(clock);

        userAuthRepository.insertVerifiedIdentityUser(new UserAuthRepository.NewVerifiedIdentityUser(
                normalizedCommand.username(),
                passwordHash,
                normalizedCommand.role(),
                normalizedCommand.legalName(),
                personalKeyHash,
                verifiedDocument.documentType(),
                verifiedDocument.normalizedOcrText(),
                now
        ));

        final String issuedAtIso = now.toString();
        return new IdentityRegistrationResult(
                true,
                null,
                rawKey,
                personalKeyService.buildDownloadFilename(normalizedCommand.username()),
                personalKeyService.buildDownloadContents(new PersonalKeyDocument(
                        normalizedCommand.username(),
                        normalizedCommand.legalName(),
                        normalizedCommand.role(),
                        rawKey,
                        issuedAtIso
                )),
                issuedAtIso,
                normalizedCommand.username(),
                normalizedCommand.role().name(),
                normalizedCommand.legalName()
        );
    }

    @Transactional
    public RotatedPersonalKeyResult rotatePersonalKey(final long userId) {
        final var user = userAuthRepository.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "user_not_found"));

        final String rawKey = personalKeyService.generateRawKey();
        final String hashed = passwordEncoder.encode(rawKey);
        final Instant now = Instant.now(clock);
        userSecurityRepository.updatePersonalKey(userId, hashed, now);

        final String issuedAtIso = now.toString();
        return new RotatedPersonalKeyResult(
                true,
                rawKey,
                personalKeyService.buildDownloadFilename(user.username()),
                personalKeyService.buildDownloadContents(new PersonalKeyDocument(
                        user.username(),
                        user.legalName(),
                        user.role(),
                        rawKey,
                        issuedAtIso
                )),
                issuedAtIso
        );
    }

    @Transactional
    public void verifyEmail(final String username, final String tokenString) {
        final Instant now = Instant.now(clock);
        final Optional<UUID> token = RegistrationSupport.parseOptionalUuid(tokenString);

        if (token.isPresent()) {
            final long userId = emailVerificationRepository.consumeIfValid(token.get(), now)
                    .orElseThrow(() -> new AuthException(HttpStatus.BAD_REQUEST, "invalid_or_expired_token"));
            userAuthRepository.setEmailVerified(userId, true);
            return;
        }

        final String safeUsername = RegistrationSupport.normalizeUsername(username);
        final long userId = emailVerificationRepository.consumeByUsernameAndCodeIfValid(safeUsername, tokenString, now)
                .orElseThrow(() -> new AuthException(HttpStatus.BAD_REQUEST, "invalid_or_expired_token"));

        userAuthRepository.setEmailVerified(userId, true);
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

    static final class RegistrationSupport {
        private RegistrationSupport() {
        }

        static boolean shouldUseDemoCode(final AuthProperties props, final Role role) {
            return props.isDemoStaticCodesEnabled() && role != Role.ADMIN;
        }

        static IdentityRegistrationCommand normalizeRegistrationCommand(final IdentityRegistrationCommand command) {
            final Role safeRole = (command.role() == null) ? Role.BUYER : command.role();
            return new IdentityRegistrationCommand(
                    normalizeUsername(command.username()),
                    safe(command.password()),
                    safe(command.confirmPassword()),
                    safe(command.legalName()),
                    safeRole,
                    command.documentType(),
                    command.documentExtractedText(),
                    command.documentImage()
            );
        }

        static void validateNewIdentityRegistration(
                final IdentityRegistrationCommand command,
                final int minPasswordLength
        ) {
            validateRequiredFields(command);
            validateRole(command);
            validatePasswordMatch(command);
            validatePasswordLength(command, minPasswordLength);
        }

        private static void validateRequiredFields(final IdentityRegistrationCommand command) {
            if (command.username().isBlank()) {
                throw new AuthException(HttpStatus.BAD_REQUEST, ERROR_INVALID_INPUT);
            }
            if (command.password().isBlank()) {
                throw new AuthException(HttpStatus.BAD_REQUEST, ERROR_INVALID_INPUT);
            }
            if (command.confirmPassword().isBlank()) {
                throw new AuthException(HttpStatus.BAD_REQUEST, ERROR_INVALID_INPUT);
            }
            if (command.legalName().isBlank()) {
                throw new AuthException(HttpStatus.BAD_REQUEST, ERROR_INVALID_INPUT);
            }
        }

        private static void validateRole(final IdentityRegistrationCommand command) {
            if (command.role() == Role.ADMIN) {
                throw new AuthException(HttpStatus.BAD_REQUEST, "invalid_role");
            }
        }

        private static void validatePasswordMatch(final IdentityRegistrationCommand command) {
            if (!command.password().equals(command.confirmPassword())) {
                throw AuthException.of(AuthError.PASSWORD_MISMATCH);
            }
        }

        private static void validatePasswordLength(
                final IdentityRegistrationCommand command,
                final int minPasswordLength
        ) {
            if (command.password().length() < minPasswordLength) {
                throw AuthException.of(AuthError.PASSWORD_TOO_SHORT);
            }
        }

        static Optional<UUID> parseOptionalUuid(final String raw) {
            if (raw == null || raw.isBlank()) {
                return Optional.empty();
            }
            try {
                return Optional.of(UUID.fromString(raw.trim()));
            } catch (IllegalArgumentException ex) {
                return Optional.empty();
            }
        }

        static String normalizeUsername(final String username) {
            return (username == null) ? "" : username.trim().toLowerCase(Locale.ROOT);
        }

        static String safe(final String value) {
            return (value == null) ? "" : value;
        }
    }
}