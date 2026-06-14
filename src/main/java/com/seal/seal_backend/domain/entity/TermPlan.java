package com.seal.seal_backend.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import com.seal.seal_backend.domain.enums.TermType;
@Entity @Table(name = "term_plans")
@Getter @Setter @NoArgsConstructor
public class TermPlan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private TermType term;
    @Column(name = "year", nullable = false)
    private Integer year;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "discipline_id", nullable = false)
    private Discipline discipline;
    @Column(name = "max_events", nullable = false)
    private Integer maxEvents = 1;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by")
    private User createdBy;
    @CreationTimestamp @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
