package com.seal.seal_backend.common.audit;

import com.seal.seal_backend.domain.entity.AuditLog;
import com.seal.seal_backend.domain.entity.User;
import com.seal.seal_backend.domain.repository.AuditLogRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cross-cutting audit logging (BR-AUD-01/02). Inject this anywhere and call log(...).
 * Owned by Lead. audit_logs are append-only (DB triggers block UPDATE/DELETE).
 *
 * Example:
 *   audit.log(actor, AuditAction.SCORE_SUBMITTED, "EVALUATION", evalId, null, newJson, null, ip);
 */
@Component
public class AuditPublisher {

    private final AuditLogRepository repo;

    public AuditPublisher(AuditLogRepository repo) { this.repo = repo; }

    /** Writes in its own transaction so the audit survives even if the caller rolls back. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(User actor, AuditAction action, String targetType, Long targetId,
                    String oldValueJson, String newValueJson, String reason, String ip) {
        AuditLog e = new AuditLog();
        e.setActor(actor);
        e.setActionType(action.name());
        e.setTargetType(targetType);
        e.setTargetId(targetId);
        e.setOldValue(oldValueJson);
        e.setNewValue(newValueJson);
        e.setReason(reason);
        e.setIpAddress(ip);
        repo.save(e);
    }
}
