package com.seal.seal_backend.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import com.seal.seal_backend.domain.enums.BudgetStatus;
@Entity @Table(name = "event_budgets")
@Getter @Setter @NoArgsConstructor
public class EventBudget {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "event_id", nullable = false)
    private Event event;
    @Column(nullable = false, length = 10) private String currency = "VND";
    @Column(name = "total_estimated_cost", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalEstimatedCost = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private BudgetStatus status = BudgetStatus.DRAFT;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "approved_by")
    private User approvedBy;
    @Column(name = "approved_at") private LocalDateTime approvedAt;
    @CreationTimestamp @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
