package com.seal.seal_backend.domain.repository;

import com.seal.seal_backend.domain.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByTargetTypeAndTargetIdOrderByCreatedAtAscIdAsc(String targetType, Long targetId);

    List<AuditLog> findByTargetTypeAndTargetIdInOrderByCreatedAtAscIdAsc(String targetType, Collection<Long> targetIds);
}
