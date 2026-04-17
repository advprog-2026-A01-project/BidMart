package id.ac.ui.cs.advprog.backend.auth.service;

import id.ac.ui.cs.advprog.backend.auth.model.AuthException;
import id.ac.ui.cs.advprog.backend.auth.model.AuthProperties;
import id.ac.ui.cs.advprog.backend.auth.repository.CaptchaRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

@Service
public class CaptchaService {

    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private final CaptchaRepository captchaRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties props;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public CaptchaService(
            final CaptchaRepository captchaRepository,
            final PasswordEncoder passwordEncoder,
            final AuthProperties props,
            final Clock clock
    ) {
        this.captchaRepository = captchaRepository;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
        this.clock = clock;
    }

    public CaptchaChallenge issueChallenge() {
        cleanup();

        if (!props.isCaptchaEnabled()) {
            return new CaptchaChallenge(null, false, null, null, 0, null);
        }

        final Instant now = Instant.now(clock);
        final Instant expiresAt = now.plusSeconds(props.getCaptchaTtlSeconds());
        final UUID id = UUID.randomUUID();
        final String answer = randomAnswer(props.getCaptchaLength());
        final String answerHash = passwordEncoder.encode(normalizeAnswer(answer));

        captchaRepository.create(id, "TEXT", "Type the characters shown in the image", answerHash, expiresAt, now);

        final String svg = renderSvg(answer, props.getCaptchaNoiseLines());
        final String dataUri = "data:image/svg+xml;base64," +
                Base64.getEncoder().encodeToString(svg.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        return new CaptchaChallenge(
                id.toString(),
                true,
                "Type the characters shown in the image",
                dataUri,
                props.getCaptchaTtlSeconds(),
                props.isDevExposeTokens() ? answer : null
        );
    }

    public void verify(final String captchaIdRaw, final String captchaAnswerRaw) {
        if (!props.isCaptchaEnabled()) {
            return;
        }

        if (captchaIdRaw == null || captchaIdRaw.isBlank() || captchaAnswerRaw == null || captchaAnswerRaw.isBlank()) {
            throw AuthException.captchaRequired();
        }

        final UUID captchaId;
        try {
            captchaId = UUID.fromString(captchaIdRaw.trim());
        } catch (IllegalArgumentException ex) {
            throw AuthException.captchaInvalid();
        }

        final var row = captchaRepository.findById(captchaId).orElseThrow(AuthException::captchaInvalid);
        final Instant now = Instant.now(clock);

        if (row.usedAt() != null) {
            throw AuthException.captchaInvalid();
        }
        if (row.expiresAt() == null || row.expiresAt().toInstant().isBefore(now)) {
            throw AuthException.captchaExpired();
        }

        final String normalizedAnswer = normalizeAnswer(captchaAnswerRaw);
        if (!passwordEncoder.matches(normalizedAnswer, row.answerHash())) {
            throw AuthException.captchaInvalid();
        }

        captchaRepository.markUsed(captchaId, now);
        cleanup();
    }

    private void cleanup() {
        final Instant now = Instant.now(clock);
        captchaRepository.deleteExpiredBefore(now.minusSeconds(1));
        captchaRepository.deleteUsedBefore(now.minusSeconds(Math.max(30, props.getCaptchaTtlSeconds())));
    }

    private String randomAnswer(final int length) {
        final StringBuilder out = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            out.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return out.toString();
    }

    private String normalizeAnswer(final String raw) {
        final String value = raw == null ? "" : raw.trim();
        return props.isCaptchaCaseSensitive() ? value : value.toUpperCase(Locale.ROOT);
    }

    private String renderSvg(final String answer, final int noiseLines) {
        final int width = 180;
        final int height = 64;

        final StringBuilder lines = new StringBuilder();
        for (int i = 0; i < noiseLines; i++) {
            final int x1 = random.nextInt(width);
            final int y1 = random.nextInt(height);
            final int x2 = random.nextInt(width);
            final int y2 = random.nextInt(height);
            final int stroke = 1 + random.nextInt(2);
            lines.append("<line x1=\"").append(x1).append("\" y1=\"").append(y1)
                    .append("\" x2=\"").append(x2).append("\" y2=\"").append(y2)
                    .append("\" stroke=\"#94a3b8\" stroke-width=\"").append(stroke).append("\" opacity=\"0.65\"/>");
        }

        final StringBuilder chars = new StringBuilder();
        for (int i = 0; i < answer.length(); i++) {
            final int x = 22 + (i * 28);
            final int y = 38 + random.nextInt(12);
            final int rotate = -12 + random.nextInt(25);
            chars.append("<text x=\"").append(x).append("\" y=\"").append(y)
                    .append("\" font-size=\"30\" font-family=\"Arial, sans-serif\" font-weight=\"700\" fill=\"#0f172a\"")
                    .append(" transform=\"rotate(").append(rotate).append(' ').append(x).append(' ').append(y).append(")\">")
                    .append(answer.charAt(i))
                    .append("</text>");
        }

        return """
                <svg xmlns="http://www.w3.org/2000/svg" width="%d" height="%d" viewBox="0 0 %d %d">
                  <rect width="100%%" height="100%%" rx="10" ry="10" fill="#f8fafc" stroke="#cbd5e1"/>
                  %s
                  %s
                </svg>
                """.formatted(width, height, width, height, lines.toString(), chars.toString());
    }

    public record CaptchaChallenge(
            String captchaId,
            boolean enabled,
            String prompt,
            String svgDataUri,
            int expiresIn,
            String devAnswer
    ) {}

}
