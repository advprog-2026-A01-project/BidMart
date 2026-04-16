package id.ac.ui.cs.advprog.backend.auth.service;

import id.ac.ui.cs.advprog.backend.auth.model.AuthException;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ClientOcrIdentityDocumentValidationService implements IdentityDocumentValidationService {

    private static final long MAX_UPLOAD_BYTES = 5L * 1024L * 1024L;

    @Override
    public VerifiedIdentityDocument validate(
            final String legalName,
            final String documentType,
            final String ocrText,
            final MultipartFile documentImage
    ) {
        final String normalizedName = normalizeStrict(legalName);
        final String normalizedOcr = normalizeStrict(ocrText);
        final String normalizedType = normalizeDocType(documentType);

        if (documentImage == null || documentImage.isEmpty()) {
            throw AuthException.identityDocumentRequired();
        }
        if (documentImage.getSize() > MAX_UPLOAD_BYTES) {
            throw AuthException.identityDocumentInvalid();
        }

        final String contentType = safe(documentImage.getContentType());
        final String filename = safe(documentImage.getOriginalFilename()).toLowerCase(Locale.ROOT);
        final boolean imageContentType = contentType.startsWith("image/");
        final boolean imageExtension = filename.endsWith(".png") || filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".webp");
        if (!imageContentType && !imageExtension) {
            throw AuthException.identityDocumentInvalid();
        }

        if (normalizedName.isBlank()) {
            throw AuthException.identityNameMismatch();
        }
        if (normalizedOcr.isBlank()) {
            throw AuthException.ocrTextMissing();
        }
        if (!containsAllNameTokensStrict(normalizedName, normalizedOcr)) {
            throw AuthException.identityNameMismatch();
        }

        return new VerifiedIdentityDocument(normalizedName, normalizedOcr, normalizedType);
    }

    private static boolean containsAllNameTokensStrict(final String normalizedName, final String normalizedOcr) {
        final List<String> nameTokens = Arrays.stream(normalizedName.split(" "))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .filter(s -> s.length() >= 2)
                .toList();

        final List<String> ocrTokens = Arrays.stream(normalizedOcr.split(" "))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .filter(s -> s.length() >= 2)
                .toList();

        if (nameTokens.isEmpty() || ocrTokens.isEmpty()) {
            return false;
        }

        return nameTokens.stream().allMatch(ocrTokens::contains);
    }

    private static String normalizeDocType(final String raw) {
        final String normalized = safe(raw).trim().toUpperCase(Locale.ROOT);
        if (!"KTP".equals(normalized) && !"KTM".equals(normalized)) {
            throw AuthException.identityDocumentInvalid();
        }
        return normalized;
    }

    private static String normalizeStrict(final String raw) {
        final String noAccent = Normalizer.normalize(safe(raw), Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "");

        return noAccent
                .replaceAll("[^A-Za-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String safe(final String raw) {
        return (raw == null) ? "" : raw;
    }
}