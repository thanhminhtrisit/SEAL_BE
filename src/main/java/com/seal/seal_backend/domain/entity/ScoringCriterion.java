package com.seal.seal_backend.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity @Table(name = "scoring_criteria")
@Getter @Setter @NoArgsConstructor
public class ScoringCriterion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "criteria_set_id", nullable = false)
    private CriteriaSet criteriaSet;
    @Column(nullable = false, length = 150) private String name;
    @Lob private String description;
    @Column(name = "max_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxScore = new BigDecimal("10.00");
    @Column(nullable = false, precision = 5, scale = 2) private BigDecimal weight;
    @Column(name = "display_order", nullable = false) private Integer displayOrder = 1;
    @Column(name = "is_active", nullable = false) private Boolean isActive = true;
    @CreationTimestamp @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
