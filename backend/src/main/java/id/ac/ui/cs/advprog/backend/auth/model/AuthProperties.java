package id.ac.ui.cs.advprog.backend.auth.model;

import org.springframework.boot.context.properties.ConfigurationProperties;

@SuppressWarnings("PMD.DataClass")
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    private int accessTtlMinutes = 60;
    private int refreshTtlDays = 14;

    private int maxSessionsPerUser = 5;
    private SessionOverflowPolicy overflowPolicy = SessionOverflowPolicy.REVOKE_OLDEST;

    private int emailVerifyTtlMinutes = 24 * 60;
    private int mfaChallengeTtlSeconds = 300;
    private int mfaMaxAttempts = 5;

    private boolean devExposeTokens = true;

    private boolean demoStaticCodesEnabled = true;
    private String demoEmailVerificationCode = "112233";
    private String demoEmailOtpCode = "445566";

    private boolean captchaEnabled = true;
    private int captchaTtlSeconds = 180;
    private int captchaLength = 5;
    private int captchaNoiseLines = 5;
    private boolean captchaCaseSensitive = false;

    public int getAccessTtlMinutes() { return accessTtlMinutes; }
    public void setAccessTtlMinutes(final int v) { this.accessTtlMinutes = v; }

    public int getRefreshTtlDays() { return refreshTtlDays; }
    public void setRefreshTtlDays(final int v) { this.refreshTtlDays = v; }

    public int getMaxSessionsPerUser() { return maxSessionsPerUser; }
    public void setMaxSessionsPerUser(final int v) { this.maxSessionsPerUser = v; }

    public SessionOverflowPolicy getOverflowPolicy() { return overflowPolicy; }
    public void setOverflowPolicy(final SessionOverflowPolicy v) { this.overflowPolicy = v; }

    public int getEmailVerifyTtlMinutes() { return emailVerifyTtlMinutes; }
    public void setEmailVerifyTtlMinutes(final int v) { this.emailVerifyTtlMinutes = v; }

    public int getMfaChallengeTtlSeconds() { return mfaChallengeTtlSeconds; }
    public void setMfaChallengeTtlSeconds(final int v) { this.mfaChallengeTtlSeconds = v; }

    public int getMfaMaxAttempts() { return mfaMaxAttempts; }
    public void setMfaMaxAttempts(final int v) { this.mfaMaxAttempts = v; }

    public boolean isDevExposeTokens() { return devExposeTokens; }
    public void setDevExposeTokens(final boolean v) { this.devExposeTokens = v; }

    public boolean isDemoStaticCodesEnabled() { return demoStaticCodesEnabled; }
    public void setDemoStaticCodesEnabled(final boolean v) { this.demoStaticCodesEnabled = v; }

    public String getDemoEmailVerificationCode() { return demoEmailVerificationCode; }
    public void setDemoEmailVerificationCode(final String v) { this.demoEmailVerificationCode = v; }

    public String getDemoEmailOtpCode() { return demoEmailOtpCode; }
    public void setDemoEmailOtpCode(final String v) { this.demoEmailOtpCode = v; }

    public boolean isCaptchaEnabled() { return captchaEnabled; }
    public void setCaptchaEnabled(final boolean captchaEnabled) { this.captchaEnabled = captchaEnabled; }

    public int getCaptchaTtlSeconds() { return captchaTtlSeconds; }
    public void setCaptchaTtlSeconds(final int captchaTtlSeconds) { this.captchaTtlSeconds = captchaTtlSeconds; }

    public int getCaptchaLength() { return captchaLength; }
    public void setCaptchaLength(final int captchaLength) { this.captchaLength = captchaLength; }

    public int getCaptchaNoiseLines() { return captchaNoiseLines; }
    public void setCaptchaNoiseLines(final int captchaNoiseLines) { this.captchaNoiseLines = captchaNoiseLines; }

    public boolean isCaptchaCaseSensitive() { return captchaCaseSensitive; }
    public void setCaptchaCaseSensitive(final boolean captchaCaseSensitive) { this.captchaCaseSensitive = captchaCaseSensitive; }

    public enum SessionOverflowPolicy {
        REJECT,
        REVOKE_OLDEST
    }
}