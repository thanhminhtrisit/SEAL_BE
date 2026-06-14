package com.seal.seal_backend.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity @Table(name = "scores")
@Getter @Setter @NoArgsConstructor
public class Score {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "evaluation_id", nullable = false)
    private Evaluation evaluation;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "criterion_id", nullable = false)
    private ScoringCriterion criterion;
    @Column(name = "score_value", nullable = false, precision = 5, scale = 2)
    private BigDecimal scoreValue;
    @Lob private String comment;
    @CreationTimestamp @Column(name = "scored_at", updatable = false)
    private LocalDateTime scoredAt;
    @UpdateTimestamp @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
