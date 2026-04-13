package id.ac.ui.cs.advprog.backend.auth.util;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@EnableWebSecurity
public final class TotpUtil {

    private static final String HMAC_ALG = "HmacSHA1";

    private static final int DEFAULT_DIGITS = 6;
    private static final int DEFAULT_PERIOD_SECONDS = 30;
    private static final int DEFAULT_SKEW_STEPS = 1;

    private static final int ZERO = 0;
    private static final int ONE = 1;
    private static final int FIVE = 5;
    private static final int EIGHT = 8;
    private static final int TWENTY_FOUR = 24;
    private static final int NEG_ONE = -1;
    private static final int BYTE_MASK = 0xFF;

    private static final String EMPTY = "";
    private static final Pattern CODE_PATTERN = Pattern.compile("^\\d{6,8}$");

    private static final char[] B32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
    private static final int[] B32_INV = buildInv();

    private TotpUtil() {}

    public static String generateBase32Secret(final int numBytes) {
        final byte[] buf = new byte[numBytes];
        new SecureRandom().nextBytes(buf);
        return base32Encode(buf);
    }

    public static String otpauthUri(final String issuer, final String account, final String base32Secret) {
        final String safeIssuer = urlEncode(issuer);
        final String safeAccount = urlEncode(account);
        final String safeSecret = urlEncode(base32Secret);

        return "otpauth://totp/" + safeIssuer + ":" + safeAccount
                + "?secret=" + safeSecret
                + "&issuer=" + safeIssuer
                + "&digits=" + DEFAULT_DIGITS
                + "&period=" + DEFAULT_PERIOD_SECONDS;
    }

    public static boolean verifyCode(final String base32Secret, final String code, final Instant now) {
        final String normalized = (code == null) ? EMPTY : code.trim();
        if (!CODE_PATTERN.matcher(normalized).matches()) return false;

        final int digits = normalized.length();
        final long counter = now.getEpochSecond() / DEFAULT_PERIOD_SECONDS;

        for (int i = -DEFAULT_SKEW_STEPS; i <= DEFAULT_SKEW_STEPS; i++) {
            final String expected = generateTotp(base32Secret, counter + i, digits);
            if (expected.equals(normalized)) return true;
        }
        return false;
    }

    private static String generateTotp(final String base32Secret, final long counter, final int digits) {
        final byte[] key = base32Decode(base32Secret);
        final byte[] msg = ByteBuffer.allocate(EIGHT).putLong(counter).array();

        final byte[] hash = hmacSha1(key, msg);
        final int offset = hash[hash.length - ONE] & 0x0F;

        final int binary =
                ((hash[offset] & 0x7F) << TWENTY_FOUR)
                        | ((hash[offset + ONE] & BYTE_MASK) << 16)
                        | ((hash[offset + 2] & BYTE_MASK) << EIGHT)
                        | (hash[offset + 3] & BYTE_MASK);

        final int mod = (int) Math.pow(10, digits);
        final int otp = binary % mod;
        return String.format(Locale.ROOT, "%0" + digits + "d", otp);
    }

    private static byte[] hmacSha1(final byte[] key, final byte[] msg) {
        try {
            final Mac mac = Mac.getInstance(HMAC_ALG);
            mac.init(new SecretKeySpec(key, HMAC_ALG));
            return mac.doFinal(msg);
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("totp_hmac_failed", ex);
        }
    }

    private static int[] buildInv() {
        final int[] inv = new int[256];
        for (int i = ZERO; i < inv.length; i++) inv[i] = NEG_ONE;
        for (int i = ZERO; i < B32.length; i++) inv[B32[i]] = i;
        return inv;
    }

    private static String base32Encode(final byte[] data) {
        final StringBuilder sb = new StringBuilder((data.length * EIGHT + 4) / FIVE);
        int buffer = ZERO;
        int bitsLeft = ZERO;

        for (byte b : data) {
            buffer = (buffer << EIGHT) | (b & BYTE_MASK);
            bitsLeft += EIGHT;

            while (bitsLeft >= FIVE) {
                final int idx = (buffer >> (bitsLeft - FIVE)) & 0x1F;
                bitsLeft -= FIVE;
                sb.append(B32[idx]);
            }
        }

        if (bitsLeft > ZERO) {
            final int idx = (buffer << (FIVE - bitsLeft)) & 0x1F;
            sb.append(B32[idx]);
        }

        return sb.toString();
    }

    private static byte[] base32Decode(final String base32) {
        final String s = (base32 == null) ? EMPTY : base32.trim().replace("=", EMPTY).toUpperCase(Locale.ROOT);
        int buffer = ZERO;
        int bitsLeft = ZERO;

        final byte[] out = new byte[(s.length() * FIVE) / EIGHT];
        int outPos = ZERO;

        for (int i = ZERO; i < s.length(); i++) {
            final char c = s.charAt(i);
            final int val = (c < 256) ? B32_INV[c] : NEG_ONE;
            if (val < ZERO) continue;

            buffer = (buffer << FIVE) | val;
            bitsLeft += FIVE;

            if (bitsLeft >= EIGHT) {
                out[outPos++] = (byte) ((buffer >> (bitsLeft - EIGHT)) & BYTE_MASK);
                bitsLeft -= EIGHT;
            }
        }

        if (outPos == out.length) return out;

        final byte[] trimmed = new byte[outPos];
        System.arraycopy(out, ZERO, trimmed, ZERO, outPos);
        return trimmed;
    }

    private static String urlEncode(final String s) {
        if (s == null) return EMPTY;
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ...
            .authorizeHttpRequests(auth -> auth
                // 👇 BERIKAN IZIN AKSES PUBLIK UNTUK FILE FRONTEND 👇
                .requestMatchers("/", "/index.html", "/assets/**", "/vite.svg").permitAll()
                
                // Endpoint API kamu yang lain biarkan seperti semula
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().authenticated()
            );
            // ...
        
        return http.build();
    }
}