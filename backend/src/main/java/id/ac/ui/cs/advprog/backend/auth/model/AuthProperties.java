package id.ac.ui.cs.advprog.backend.auth.model;

import org.springframework.boot.context.properties.ConfigurationProperties;

/*
Tanggung jawab: konfigurasi auth (TTL token, max session, policy).
 */
@SuppressWarnings("PMD.DataClass")
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    private int accessTtlMinutes = 60;
    private int refreshTtlDays = 14;

    private int maxSessionsPerUser = 5;
    private SessionOverflowPolicy overflowPolicy = SessionOverflowPolicy.REVOKE_OLDEST;

    // new
    private int emailVerifyTtlMinutes = 24 * 60;     // 24h
    private int mfaChallengeTtlSeconds = 300;        // 5m
    private int mfaMaxAttempts = 5;

    // dev helper: expose verification token and email OTP in API response (optional)
    private boolean devExposeTokens = true;

    // Demo static codes for non-admin users
    private boolean demoStaticCodesEnabled = true;
    private String demoEmailVerificationCode = "112233";
    private String demoEmailOtpCode = "445566";

    public int getAccessTtlMinutes() {
        return accessTtlMinutes;
    }

    public void setAccessTtlMinutes(final int v) {
        this.accessTtlMinutes = v;
    }

    public int getRefreshTtlDays() {
        return refreshTtlDays;
    }

    public void setRefreshTtlDays(final int v) {
        this.refreshTtlDays = v;
    }

    public int getMaxSessionsPerUser() {
        return maxSessionsPerUser;
    }

    public void setMaxSessionsPerUser(final int v) {
        this.maxSessionsPerUser = v;
    }

    public SessionOverflowPolicy getOverflowPolicy() {
        return overflowPolicy;
    }

    public void setOverflowPolicy(final SessionOverflowPolicy v) {
        this.overflowPolicy = v;
    }

    public int getEmailVerifyTtlMinutes() {
        return emailVerifyTtlMinutes;
    }

    public void setEmailVerifyTtlMinutes(final int v) {
        this.emailVerifyTtlMinutes = v;
    }

    public int getMfaChallengeTtlSeconds() {
        return mfaChallengeTtlSeconds;
    }

    public void setMfaChallengeTtlSeconds(final int v) {
        this.mfaChallengeTtlSeconds = v;
    }

    public int getMfaMaxAttempts() {
        return mfaMaxAttempts;
    }

    public void setMfaMaxAttempts(final int v) {
        this.mfaMaxAttempts = v;
    }

    public boolean isDevExposeTokens() {
        return devExposeTokens;
    }

    public void setDevExposeTokens(final boolean v) {
        this.devExposeTokens = v;
    }

    // Kebutuhan kode OTP dan MFA
    public boolean isDemoStaticCodesEnabled() {
        return demoStaticCodesEnabled;
    }

    public void setDemoStaticCodesEnabled(final boolean v) {
        this.demoStaticCodesEnabled = v;
    }

    public String getDemoEmailVerificationCode() {
        return demoEmailVerificationCode;
    }

    public void setDemoEmailVerificationCode(final String v) {
        this.demoEmailVerificationCode = v;
    }

    public String getDemoEmailOtpCode() {
        return demoEmailOtpCode;
    }

    public void setDemoEmailOtpCode(final String v) {
        this.demoEmailOtpCode = v;
    }

    public enum SessionOverflowPolicy {
        REJECT,
        REVOKE_OLDEST
    }
}