package com.seal.seal_backend.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import com.seal.seal_backend.domain.enums.TeamStatus;
@Entity @Table(name = "teams")
@Getter @Setter @NoArgsConstructor
public class Team {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "event_id", nullable = false)
    private Event event;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "leader_id", nullable = false)
    private User leader;
    @Column(nullable = false, length = 150) private String name;
    @Lob private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private TeamStatus status = TeamStatus.REGISTERED;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "approved_by")
    private User approvedBy;
    @Column(name = "approved_at") private LocalDateTime approvedAt;
    @Lob @Column(name = "rejection_reason") private String rejectionReason;
    @Lob @Column(name = "disqualified_reason") private String disqualifiedReason;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "disqualified_by")
    private User disqualifiedBy;
    @Column(name = "disqualified_at") private LocalDateTime disqualifiedAt;
    @CreationTimestamp @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
