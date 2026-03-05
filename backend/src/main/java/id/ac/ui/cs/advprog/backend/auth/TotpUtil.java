package id.ac.ui.cs.advprog.backend.auth;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class TotpUtil {

    private static final String HMAC_ALG = "HmacSHA1";
    private static final int DEFAULT_DIGITS = 6;
    private static final int DEFAULT_PERIOD_SECONDS = 30;
    private static final int DEFAULT_SKEW_STEPS = 1; // allow -1,0,+1 time step

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
        // Standard otpauth URI (works for Google Authenticator / Authy / etc)
        return "otpauth://totp/" + safeIssuer + ":" + safeAccount
                + "?secret=" + safeSecret
                + "&issuer=" + safeIssuer
                + "&digits=" + DEFAULT_DIGITS
                + "&period=" + DEFAULT_PERIOD_SECONDS;
    }

    public static boolean verifyCode(final String base32Secret, final String code, final Instant now) {
        final String normalized = (code == null) ? "" : code.trim();
        if (!normalized.matches("\\d{6,8}")) return false;

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
        final byte[] msg = ByteBuffer.allocate(8).putLong(counter).array();

        final byte[] hash = hmacSha1(key, msg);
        final int offset = hash[hash.length - 1] & 0x0F;

        final int binary =
                ((hash[offset] & 0x7F) << 24)
                        | ((hash[offset + 1] & 0xFF) << 16)
                        | ((hash[offset + 2] & 0xFF) << 8)
                        | (hash[offset + 3] & 0xFF);

        final int mod = (int) Math.pow(10, digits);
        final int otp = binary % mod;
        return String.format(Locale.ROOT, "%0" + digits + "d", otp);
    }

    private static byte[] hmacSha1(final byte[] key, final byte[] msg) {
        try {
            final Mac mac = Mac.getInstance(HMAC_ALG);
            mac.init(new SecretKeySpec(key, HMAC_ALG));
            return mac.doFinal(msg);
        } catch (Exception ex) {
            throw new IllegalStateException("totp_hmac_failed", ex);
        }
    }

    // ===== Base32 (RFC 4648) =====

    private static int[] buildInv() {
        final int[] inv = new int[256];
        for (int i = 0; i < inv.length; i++) inv[i] = -1;
        for (int i = 0; i < B32.length; i++) {
            inv[B32[i]] = i;
        }
        return inv;
    }

    private static String base32Encode(final byte[] data) {
        final StringBuilder sb = new StringBuilder((data.length * 8 + 4) / 5);
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                final int idx = (buffer >> (bitsLeft - 5)) & 0x1F;
                bitsLeft -= 5;
                sb.append(B32[idx]);
            }
        }
        if (bitsLeft > 0) {
            final int idx = (buffer << (5 - bitsLeft)) & 0x1F;
            sb.append(B32[idx]);
        }
        return sb.toString();
    }

    private static byte[] base32Decode(final String base32) {
        final String s = (base32 == null) ? "" : base32.trim().replace("=", "").toUpperCase(Locale.ROOT);
        int buffer = 0;
        int bitsLeft = 0;

        final byte[] out = new byte[(s.length() * 5) / 8];
        int outPos = 0;

        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            final int val = (c < 256) ? B32_INV[c] : -1;
            if (val < 0) continue;

            buffer = (buffer << 5) | val;
            bitsLeft += 5;

            if (bitsLeft >= 8) {
                out[outPos++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }

        if (outPos == out.length) return out;

        final byte[] trimmed = new byte[outPos];
        System.arraycopy(out, 0, trimmed, 0, outPos);
        return trimmed;
    }

    private static String urlEncode(final String s) {
        if (s == null) return "";
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}