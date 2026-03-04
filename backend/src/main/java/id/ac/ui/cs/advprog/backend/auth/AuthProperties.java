package id.ac.ui.cs.advprog.backend.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    // Access token TTL in minutes (opaque UUID token stored in DB)
    private int accessTtlMinutes = 60;

    // Refresh token TTL in days
    private int refreshTtlDays = 14;

    /** Maximum active sessions per user. Set <=0 to disable the limit. */
    private int maxSessionsPerUser = 5;

    /** What to do when max session is exceeded. */
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
