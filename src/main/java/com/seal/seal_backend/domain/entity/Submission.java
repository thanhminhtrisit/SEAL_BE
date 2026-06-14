package com.seal.seal_backend.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import com.seal.seal_backend.domain.enums.SubmissionStatus;
@Entity @Table(name = "submissions")
@Getter @Setter @NoArgsConstructor
public class Submission {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "team_id", nullable = false)
    private Team team;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "round_id", nullable = false)
    private Round round;
    // Plain FK id to avoid a circular mapping with SubmissionVersion.
    @Column(name = "current_version_id") private Long currentVersionId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private SubmissionStatus status = SubmissionStatus.DRAFT;
    @Column(name = "submitted_at") private LocalDateTime submittedAt;
    @Column(name = "last_updated_at") private LocalDateTime lastUpdatedAt;
    @Column(name = "github_metadata", columnDefinition = "json") private String githubMetadata;
    @CreationTimestamp @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
