package id.ac.ui.cs.advprog.backend.auth;

import java.time.Clock;
import org.springframework.stereotype.Component;

@Component
public class ClockHolder {
    private final Clock clock;
    public ClockHolder(final Clock clock) {
        this.clock = clock;
    }
    public Clock clock() {
        return clock;
    }
}