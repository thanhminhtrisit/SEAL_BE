package com.seal.seal_backend.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity @Table(name = "budget_items")
@Getter @Setter @NoArgsConstructor
public class BudgetItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "budget_id", nullable = false)
    private EventBudget budget;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "category_id", nullable = false)
    private BudgetCategory category;
    @Column(nullable = false) private String description;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal quantity = BigDecimal.ONE;
    @Column(name = "unit_cost", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitCost = BigDecimal.ZERO;
    // DB-computed (GENERATED ALWAYS): read-only in JPA.
    @Column(name = "amount", insertable = false, updatable = false, precision = 15, scale = 2)
    private BigDecimal amount;
    @Lob private String notes;
    @CreationTimestamp @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
