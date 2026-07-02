package com.seal.seal_backend.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "rankings")
@Getter @Setter @NoArgsConstructor
public class Ranking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Round round;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", columnDefinition = "BIGINT UNSIGNED")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Team team;

    @Column(name = "total_score", nullable = false, precision = 8, scale = 3)
    private BigDecimal totalScore = BigDecimal.ZERO;

    // Trong SQL là INT UNSIGNED nên cần chỉ định rõ để tránh lỗi DDL
    @Column(name = "rank_position", nullable = false, columnDefinition = "INT UNSIGNED")
    private Integer rankPosition;

    @Column(name = "is_promoted", nullable = false)
    private Boolean isPromoted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "computed_by", columnDefinition = "BIGINT UNSIGNED")
    private User computedBy;

    @CreationTimestamp
    @Column(name = "computed_at", updatable = false)
    private LocalDateTime computedAt;

    @Column(name = "snapshot_note")
    private String snapshotNote;
}