package com.seal.seal_backend.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
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
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "submitted_by", nullable = false)
    private User submittedBy;
    @Column(name = "attempt_number", nullable = false) private Integer attemptNumber;
    @Column(name = "repo_url", length = 500) private String repoUrl;
    @Column(name = "demo_url", length = 500) private String demoUrl;
    @Column(name = "slide_url", length = 500) private String slideUrl;
    @Column(name = "report_url", length = 500) private String reportUrl;
    @Column(name = "change_note", length = 255) private String changeNote;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private SubmissionStatus status = SubmissionStatus.SUBMITTED;
    @Column(name = "submitted_at") private LocalDateTime submittedAt;
    @Column(name = "github_metadata", columnDefinition = "json") private String githubMetadata;
    @CreationTimestamp @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @org.hibernate.annotations.UpdateTimestamp @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
