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
@Entity @Table(name = "judge_assignments")
@Getter @Setter @NoArgsConstructor
public class JudgeAssignment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "judge_id", nullable = false)
    private User judge;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "event_id", nullable = false)
    private Event event;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "round_id", nullable = false)
    private Round round;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "category_id")
    private Category category;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "assigned_by", nullable = false)
    private User assignedBy;
    @CreationTimestamp @Column(name = "assigned_at", updatable = false)
    private LocalDateTime assignedAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private AssignmentStatus status = AssignmentStatus.ACTIVE;
}
