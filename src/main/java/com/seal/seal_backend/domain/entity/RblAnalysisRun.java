package com.seal.seal_backend.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import com.seal.seal_backend.domain.enums.RblRunStatus;
@Entity @Table(name = "rbl_analysis_runs")
@Getter @Setter @NoArgsConstructor
public class RblAnalysisRun {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "event_id", nullable = false)
    private Event event;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "round_id")
    private Round round;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "generated_by")
    private User generatedBy;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private RblRunStatus status = RblRunStatus.COMPLETED;
    @Lob private String notes;
    @CreationTimestamp @Column(name = "generated_at", updatable = false)
    private LocalDateTime generatedAt;
}
