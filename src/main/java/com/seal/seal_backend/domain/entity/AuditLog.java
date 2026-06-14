package com.seal.seal_backend.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity @Table(name = "audit_logs")
@Getter @Setter @NoArgsConstructor
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "actor_id")
    private User actor;
    @Column(name = "action_type", nullable = false, length = 100) private String actionType;
    @Column(name = "target_type", nullable = false, length = 100) private String targetType;
    @Column(name = "target_id") private Long targetId;
    @Column(name = "old_value", columnDefinition = "json") private String oldValue;
    @Column(name = "new_value", columnDefinition = "json") private String newValue;
    @Lob private String reason;
    @Column(name = "ip_address", length = 45) private String ipAddress;
    @CreationTimestamp @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
