package id.ac.ui.cs.advprog.backend.auth;

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

    public int getAccessTtlMinutes() {
        return accessTtlMinutes;
    }

    public void setAccessTtlMinutes(final int accessTtlMinutes) {
        this.accessTtlMinutes = accessTtlMinutes;
    }

    public int getRefreshTtlDays() {
        return refreshTtlDays;
    }

    public void setRefreshTtlDays(final int refreshTtlDays) {
        this.refreshTtlDays = refreshTtlDays;
    }

    public int getMaxSessionsPerUser() {
        return maxSessionsPerUser;
    }

    public void setMaxSessionsPerUser(final int maxSessionsPerUser) {
        this.maxSessionsPerUser = maxSessionsPerUser;
    }

    public SessionOverflowPolicy getOverflowPolicy() {
        return overflowPolicy;
    }

    public void setOverflowPolicy(final SessionOverflowPolicy overflowPolicy) {
        this.overflowPolicy = overflowPolicy;
    }

    public enum SessionOverflowPolicy {
        REJECT,
        REVOKE_OLDEST
    }
}