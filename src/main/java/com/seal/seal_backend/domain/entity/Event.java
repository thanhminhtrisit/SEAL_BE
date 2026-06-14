package com.seal.seal_backend.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import com.seal.seal_backend.domain.enums.EventType;
import com.seal.seal_backend.domain.enums.EventStatus;
@Entity @Table(name = "events")
@Getter @Setter @NoArgsConstructor
public class Event {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 200)
    private String name;
    @Column(nullable = false, unique = true, length = 220)
    private String slug;
    @Enumerated(EnumType.STRING) @Column(name = "event_type", nullable = false, length = 20)
    private EventType eventType;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "discipline_id", nullable = false)
    private Discipline discipline;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "term_plan_id", nullable = false)
    private TermPlan termPlan;
    @Lob private String description;
    @Column(name = "registration_start") private LocalDateTime registrationStart;
    @Column(name = "registration_end") private LocalDateTime registrationEnd;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private EventStatus status = EventStatus.DRAFT;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "owner_coordinator_id", nullable = false)
    private User ownerCoordinator;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    @Column(name = "submitted_at") private LocalDateTime submittedAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "approved_by")
    private User approvedBy;
    @Column(name = "approved_at") private LocalDateTime approvedAt;
    @Lob @Column(name = "rejection_reason") private String rejectionReason;
    @Column(name = "archived_at") private LocalDateTime archivedAt;
    @CreationTimestamp @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
