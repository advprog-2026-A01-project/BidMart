package id.ac.ui.cs.advprog.backend.auth.util;

import java.time.Clock;
import org.springframework.stereotype.Component;

@Component
public class ClockHolder {

    private final Clock clockValue;

    public ClockHolder(final Clock clock) {
        this.clockValue = clock;
    }

    public Clock clock() {
        return this.clockValue;
    }
}