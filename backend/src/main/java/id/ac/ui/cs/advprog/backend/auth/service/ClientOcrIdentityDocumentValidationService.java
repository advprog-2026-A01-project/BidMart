package id.ac.ui.cs.advprog.backend.auth.service;

import id.ac.ui.cs.advprog.backend.auth.model.AuthException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Locale;

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
        final String normalizedName = normalize(legalName);
        final String normalizedOcr = normalize(ocrText);
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
        if (!containsAllNameTokens(normalizedName, normalizedOcr)) {
            throw AuthException.identityNameMismatch();
        }

        return new VerifiedIdentityDocument(normalizedName, normalizedOcr, normalizedType);
    }

    // Method for checking identityName match or not?
    private static boolean containsAllNameTokens(final String normalizedName, final String normalizedOcr) {
        final String[] tokens = Arrays.stream(normalizedName.split(" "))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .filter(s -> s.length() >= 2)
                .toArray(String[]::new);

        if (tokens.length == 0) {
            return false;
        }

        int matched = 0;
        for (String token: tokens) {
            if (normalizedOcr.contains(token)) {
                matched++;
            }
        }
        return matched == tokens.length;
    }

    // Method for normalizing doc type
    private static String normalizeDocType(final String raw) {
        final String normalized = safe(raw).trim().toUpperCase(Locale.ROOT);
        if (!"KTP".equals(normalized) && !"KTM".equals(normalized)) {
            throw AuthException.identityDocumentInvalid();
        }
        return normalized;
    }

    // Method for normalizing the String file name
    private static String normalize(final String raw) {
        return safe(raw)
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    // Method for checking whether the filename is null or not
    private static String safe(final String raw) {
        return (raw == null) ? "" : raw;
    }
}
