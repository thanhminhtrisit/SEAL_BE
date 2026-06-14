package com.seal.seal_backend.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import com.seal.seal_backend.domain.enums.AssignmentStatus;
@Entity @Table(name = "user_role_assignments")
@Getter @Setter @NoArgsConstructor
public class UserRoleAssignment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "role_id", nullable = false)
    private Role role;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "event_id")
    private Event event;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "round_id")
    private Round round;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "category_id")
    private Category category;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "assigned_by")
    private User assignedBy;
    @CreationTimestamp @Column(name = "assigned_at", updatable = false)
    private LocalDateTime assignedAt;
    @Column(name = "revoked_at") private LocalDateTime revokedAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private AssignmentStatus status = AssignmentStatus.ACTIVE;
}
