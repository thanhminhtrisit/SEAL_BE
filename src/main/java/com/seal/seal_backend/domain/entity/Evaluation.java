package com.seal.seal_backend.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import com.seal.seal_backend.domain.enums.EvaluationStatus;
@Entity @Table(name = "evaluations")
@Getter @Setter @NoArgsConstructor
public class    Evaluation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "judge_assignment_id")
    private JudgeAssignment judgeAssignment;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "judge_id", nullable = false)
    private User judge;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "round_id", nullable = false)
    private Round round;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private EvaluationStatus status = EvaluationStatus.DRAFT;
    @Lob @Column(name = "general_comment") private String generalComment;
    @Column(name = "started_at") private LocalDateTime startedAt;
    @Column(name = "submitted_at") private LocalDateTime submittedAt;
    @Column(name = "locked_at") private LocalDateTime lockedAt;
    @CreationTimestamp @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
