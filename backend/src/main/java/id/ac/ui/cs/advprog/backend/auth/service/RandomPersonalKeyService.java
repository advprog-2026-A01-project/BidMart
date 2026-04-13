package id.ac.ui.cs.advprog.backend.auth.service;

import id.ac.ui.cs.advprog.backend.auth.model.Role;
import java.security.SecureRandom;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class RandomPersonalKeyService implements PersonalKeyService {

    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int GROUPS = 4;
    private static final int CHARS_PER_GROUP = 4;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generateRawKey() {
        final StringBuilder out = new StringBuilder(GROUPS * CHARS_PER_GROUP + (GROUPS - 1));
        for (int group = 0; group < GROUPS; group++) {
            if (group > 0) {
                out.append('-');
            }
            for (int i = 0; i < CHARS_PER_GROUP; i++) {
                out.append(ALPHABET[secureRandom.nextInt(ALPHABET.length)]);
            }
        }
        return out.toString();
    }

    @Override
    public String buildDownloadFilename(final String username) {
        final String safe = (username == null ? "user" : username.trim().toLowerCase(Locale.ROOT)).replaceAll("[^a-z0-9._-]", "_");
        return "bidmart-private-key-" + safe + ".txt";
    }

    @Override
    public String buildDownloadContents(
            final String username,
            final String legalName,
            final Role role,
            final String rawKey,
            final String issuedAtIso
    ) {
        return String.join("\n",
                "BidMart Private Key",
                "====================",
                "Username: " + safe(username),
                "Legal name: " + safe(legalName),
                "Role: " + ((role == null) ? "BUYER" : role.name()),
                "Issued at: " + safe(issuedAtIso),
                "",
                "Private key / OTP:",
                rawKey,
                "",
                "Simpan file ini baik-baik. Key ini dipakai saat login dan akan berubah kalau kamu melakukan rotate key dari akunmu.",
                "Jangan bagikan file ini ke orang lain."
        );
    }

    private static String safe(final String value) {
        return (value == null || value.isBlank()) ? "-" : value.trim();
    }
}
