package id.ac.ui.cs.advprog.backend.auth.model;

import org.springframework.boot.context.properties.ConfigurationProperties;

@SuppressWarnings("PMD.DataClass")
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    private final Token token = new Token();
    private final Session session = new Session();
    private final Mfa mfa = new Mfa();
    private final Demo demo = new Demo();
    private final Captcha captcha = new Captcha();

    public int getAccessTtlMinutes() { return token.getAccessTtlMinutes(); }
    public void setAccessTtlMinutes(final int v) { token.setAccessTtlMinutes(v); }

    public int getRefreshTtlDays() { return token.getRefreshTtlDays(); }
    public void setRefreshTtlDays(final int v) { token.setRefreshTtlDays(v); }

    public boolean isDevExposeTokens() { return token.isDevExposeTokens(); }
    public void setDevExposeTokens(final boolean v) { token.setDevExposeTokens(v); }

    public int getMaxSessionsPerUser() { return session.getMaxSessionsPerUser(); }
    public void setMaxSessionsPerUser(final int v) { session.setMaxSessionsPerUser(v); }

    public SessionOverflowPolicy getOverflowPolicy() { return session.getOverflowPolicy(); }
    public void setOverflowPolicy(final SessionOverflowPolicy v) { session.setOverflowPolicy(v); }

    public int getEmailVerifyTtlMinutes() { return mfa.getEmailVerifyTtlMinutes(); }
    public void setEmailVerifyTtlMinutes(final int v) { mfa.setEmailVerifyTtlMinutes(v); }

    public int getMfaChallengeTtlSeconds() { return mfa.getMfaChallengeTtlSeconds(); }
    public void setMfaChallengeTtlSeconds(final int v) { mfa.setMfaChallengeTtlSeconds(v); }

    public int getMfaMaxAttempts() { return mfa.getMfaMaxAttempts(); }
    public void setMfaMaxAttempts(final int v) { mfa.setMfaMaxAttempts(v); }

    public boolean isDemoStaticCodesEnabled() { return demo.isDemoStaticCodesEnabled(); }
    public void setDemoStaticCodesEnabled(final boolean v) { demo.setDemoStaticCodesEnabled(v); }

    public String getDemoEmailVerificationCode() { return demo.getDemoEmailVerificationCode(); }
    public void setDemoEmailVerificationCode(final String v) { demo.setDemoEmailVerificationCode(v); }

    public String getDemoEmailOtpCode() { return demo.getDemoEmailOtpCode(); }
    public void setDemoEmailOtpCode(final String v) { demo.setDemoEmailOtpCode(v); }

    public boolean isCaptchaEnabled() { return captcha.isCaptchaEnabled(); }
    public void setCaptchaEnabled(final boolean v) { captcha.setCaptchaEnabled(v); }

    public int getCaptchaTtlSeconds() { return captcha.getCaptchaTtlSeconds(); }
    public void setCaptchaTtlSeconds(final int v) { captcha.setCaptchaTtlSeconds(v); }

    public int getCaptchaLength() { return captcha.getCaptchaLength(); }
    public void setCaptchaLength(final int v) { captcha.setCaptchaLength(v); }

    public int getCaptchaNoiseLines() { return captcha.getCaptchaNoiseLines(); }
    public void setCaptchaNoiseLines(final int v) { captcha.setCaptchaNoiseLines(v); }

    public boolean isCaptchaCaseSensitive() { return captcha.isCaptchaCaseSensitive(); }
    public void setCaptchaCaseSensitive(final boolean v) { captcha.setCaptchaCaseSensitive(v); }

    public static class Token {
        private int accessTtlMinutes = 60;
        private int refreshTtlDays = 14;
        private boolean devExposeTokens = true;

        public int getAccessTtlMinutes() { return accessTtlMinutes; }
        public void setAccessTtlMinutes(final int v) { this.accessTtlMinutes = v; }
        public int getRefreshTtlDays() { return refreshTtlDays; }
        public void setRefreshTtlDays(final int v) { this.refreshTtlDays = v; }
        public boolean isDevExposeTokens() { return devExposeTokens; }
        public void setDevExposeTokens(final boolean v) { this.devExposeTokens = v; }
    }

    public static class Session {
        private int maxSessionsPerUser = 5;
        private SessionOverflowPolicy overflowPolicy = SessionOverflowPolicy.REVOKE_OLDEST;

        public int getMaxSessionsPerUser() { return maxSessionsPerUser; }
        public void setMaxSessionsPerUser(final int v) { this.maxSessionsPerUser = v; }
        public SessionOverflowPolicy getOverflowPolicy() { return overflowPolicy; }
        public void setOverflowPolicy(final SessionOverflowPolicy v) { this.overflowPolicy = v; }
    }

    public static class Mfa {
        private int emailVerifyTtlMinutes = 24 * 60;
        private int mfaChallengeTtlSeconds = 300;
        private int mfaMaxAttempts = 5;

        public int getEmailVerifyTtlMinutes() { return emailVerifyTtlMinutes; }
        public void setEmailVerifyTtlMinutes(final int v) { this.emailVerifyTtlMinutes = v; }
        public int getMfaChallengeTtlSeconds() { return mfaChallengeTtlSeconds; }
        public void setMfaChallengeTtlSeconds(final int v) { this.mfaChallengeTtlSeconds = v; }
        public int getMfaMaxAttempts() { return mfaMaxAttempts; }
        public void setMfaMaxAttempts(final int v) { this.mfaMaxAttempts = v; }
    }

    public static class Demo {
        private boolean demoStaticCodesEnabled = true;
        private String demoEmailVerificationCode = "112233";
        private String demoEmailOtpCode = "445566";

        public boolean isDemoStaticCodesEnabled() { return demoStaticCodesEnabled; }
        public void setDemoStaticCodesEnabled(final boolean v) { this.demoStaticCodesEnabled = v; }
        public String getDemoEmailVerificationCode() { return demoEmailVerificationCode; }
        public void setDemoEmailVerificationCode(final String v) { this.demoEmailVerificationCode = v; }
        public String getDemoEmailOtpCode() { return demoEmailOtpCode; }
        public void setDemoEmailOtpCode(final String v) { this.demoEmailOtpCode = v; }
    }

    public static class Captcha {
        private boolean captchaEnabled = true;
        private int captchaTtlSeconds = 180;
        private int captchaLength = 5;
        private int captchaNoiseLines = 5;
        private boolean captchaCaseSensitive = false;

        public boolean isCaptchaEnabled() {
            return captchaEnabled;
        }

        public void setCaptchaEnabled(final boolean v) {
            this.captchaEnabled = v;
        }
        public int getCaptchaTtlSeconds() {
            return captchaTtlSeconds;
        }
        public void setCaptchaTtlSeconds(final int v) { this.captchaTtlSeconds = v; }
        public int getCaptchaLength() { return captchaLength; }
        public void setCaptchaLength(final int v) { this.captchaLength = v; }
        public int getCaptchaNoiseLines() { return captchaNoiseLines; }
        public void setCaptchaNoiseLines(final int v) { this.captchaNoiseLines = v; }
        public boolean isCaptchaCaseSensitive() { return captchaCaseSensitive; }
        public void setCaptchaCaseSensitive(final boolean v) { this.captchaCaseSensitive = v; }
    }

    public enum SessionOverflowPolicy {
        REJECT,
        REVOKE_OLDEST
    }
}