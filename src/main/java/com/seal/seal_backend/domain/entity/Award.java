package com.seal.seal_backend.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import com.seal.seal_backend.domain.enums.AwardType;
@Entity @Table(name = "awards")
@Getter @Setter @NoArgsConstructor
public class Award {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "event_id", nullable = false)
    private Event event;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "team_id", nullable = false)
    private Team team;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "ranking_id")
    private Ranking ranking;
    @Enumerated(EnumType.STRING) @Column(name = "award_type", nullable = false, length = 50)
    private AwardType awardType;
    @Lob private String description;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "awarded_by", nullable = false)
    private User awardedBy;
    @CreationTimestamp @Column(name = "awarded_at", updatable = false)
    private LocalDateTime awardedAt;
}
