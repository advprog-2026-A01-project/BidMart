package id.ac.ui.cs.advprog.backend;

import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DbPingController {

    private final JdbcTemplate jdbcTemplate;

    public DbPingController(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/api/db/ping")
    public Map<String, Object> ping() {
        final Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        return Map.of("db", one);
    }
}