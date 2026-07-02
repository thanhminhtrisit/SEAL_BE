package com.seal.seal_backend.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import com.seal.seal_backend.domain.enums.RoundStatus;
@Entity @Table(name = "rounds")
@Getter @Setter @NoArgsConstructor
public class Round {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "event_id", nullable = false)
    private Event event;
    @Column(nullable = false, length = 150) private String name;
    @Column(name = "order_number", nullable = false) private Integer orderNumber;
    @Column(name = "submission_deadline") private LocalDateTime submissionDeadline;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private RoundStatus status = RoundStatus.DRAFT;
    @Column(name = "promotion_top_n") private Integer promotionTopN;
    @Column(name = "is_final_round", nullable = false) private Boolean isFinalRound = false;
    @Column(name = "requires_repo", nullable = false) private Boolean requiresRepo = true;
    @Column(name = "requires_demo", nullable = false) private Boolean requiresDemo = false;
    @Column(name = "requires_slide", nullable = false) private Boolean requiresSlide = false;
    @Column(name = "requires_report", nullable = false) private Boolean requiresReport = false;
    @CreationTimestamp @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
