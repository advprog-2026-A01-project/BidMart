package id.ac.ui.cs.advprog.backend.auth.service;

import id.ac.ui.cs.advprog.backend.auth.model.AuthError;
import id.ac.ui.cs.advprog.backend.auth.model.AuthException;
import id.ac.ui.cs.advprog.backend.auth.model.AuthProperties;
import id.ac.ui.cs.advprog.backend.auth.repository.SessionRepository;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SessionLimitService {

    private final SessionRepository sessionRepository;
    private final int maxSessions;
    private final AuthProperties.SessionOverflowPolicy overflowPolicy;

    public SessionLimitService(
            final SessionRepository sessionRepository,
            @Value("${auth.max-sessions-per-user:0}") final int maxSessions,
            @Value("${auth.overflow-policy:REVOKE_OLDEST}") final AuthProperties.SessionOverflowPolicy overflowPolicy
    ) {
        this.sessionRepository = sessionRepository;
        this.maxSessions = maxSessions;
        this.overflowPolicy = overflowPolicy;
    }

    public void enforce(final long userId, final Instant now) {
        if (maxSessions <= 0) return;

        final int active = sessionRepository.countActiveSessions(userId, now);
        if (active < maxSessions) return;

        if (overflowPolicy == AuthProperties.SessionOverflowPolicy.REJECT) {
            throw AuthException.of(AuthError.TOO_MANY_SESSIONS);
        }

        sessionRepository.revokeOldestSessions(userId, (active - maxSessions) + 1, now);
    }
}