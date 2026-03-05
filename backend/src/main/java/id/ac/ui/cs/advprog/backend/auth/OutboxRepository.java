package id.ac.ui.cs.advprog.backend.auth;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OutboxRepository {

    private final JdbcTemplate jdbcTemplate;

    public OutboxRepository(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static OffsetDateTime odt(final Instant t) {
        return OffsetDateTime.ofInstant(t, ZoneOffset.UTC);
    }

    public UUID append(
            final String eventType,
            final String aggregateType,
            final String aggregateId,
            final String payloadJson,
            final Instant now
    ) {
        final UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO app_outbox_events(id, event_type, aggregate_type, aggregate_id, payload_json, created_at, published_at)
                VALUES (?, ?, ?, ?, ?, ?, NULL)
                """,
                id, eventType, aggregateType, aggregateId, payloadJson, odt(now)
        );
        return id;
    }

    public List<OutboxRow> listRecent(final int limit) {
        final int safe = (limit <= 0) ? 50 : Math.min(limit, 200);
        return jdbcTemplate.query(
                """
                SELECT id, event_type, aggregate_type, aggregate_id, payload_json, created_at, published_at
                FROM app_outbox_events
                ORDER BY created_at DESC
                LIMIT ?
                """,
                (rs, n) -> new OutboxRow(
                        (UUID) rs.getObject("id"),
                        rs.getString("event_type"),
                        rs.getString("aggregate_type"),
                        rs.getString("aggregate_id"),
                        rs.getString("payload_json"),
                        rs.getObject("created_at", OffsetDateTime.class),
                        rs.getObject("published_at", OffsetDateTime.class)
                ),
                safe
        );
    }

    public record OutboxRow(
            UUID id,
            String eventType,
            String aggregateType,
            String aggregateId,
            String payloadJson,
            OffsetDateTime createdAt,
            OffsetDateTime publishedAt
    ) {}
}