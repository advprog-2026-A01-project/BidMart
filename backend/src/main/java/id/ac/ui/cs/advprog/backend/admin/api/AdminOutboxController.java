package id.ac.ui.cs.advprog.backend.admin.api;

import id.ac.ui.cs.advprog.backend.auth.repository.OutboxRepository;
import id.ac.ui.cs.advprog.backend.security.RequiresPermission;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/outbox")
public class AdminOutboxController {

    private final OutboxRepository outboxRepository;

    public AdminOutboxController(final OutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @GetMapping
    @RequiresPermission("rbac:read")
    public List<OutboxRepository.OutboxRow> recent(@RequestParam(name = "limit", required = false) final Integer limit) {
        return outboxRepository.listRecent(limit == null ? 50 : limit);
    }
}