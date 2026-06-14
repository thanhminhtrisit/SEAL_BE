package com.seal.seal_backend.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import com.seal.seal_backend.domain.enums.RblMetricType;
@Entity @Table(name = "rbl_metric_results")
@Getter @Setter @NoArgsConstructor
public class RblMetricResult {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "analysis_run_id", nullable = false)
    private RblAnalysisRun analysisRun;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "criterion_id")
    private ScoringCriterion criterion;
    @Enumerated(EnumType.STRING) @Column(name = "metric_type", nullable = false, length = 50)
    private RblMetricType metricType;
    @Column(name = "metric_value", nullable = false, precision = 10, scale = 6)
    private BigDecimal metricValue;
    @Column(name = "sample_size") private Integer sampleSize;
    @Lob private String notes;
}
