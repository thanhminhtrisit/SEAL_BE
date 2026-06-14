package com.seal.seal_backend.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity @Table(name = "submission_versions")
@Getter @Setter @NoArgsConstructor
public class SubmissionVersion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;
    @Column(name = "version_number", nullable = false) private Integer versionNumber;
    @Column(name = "repo_url", nullable = false, length = 500) private String repoUrl;
    @Column(name = "demo_url", length = 500) private String demoUrl;
    @Column(name = "slide_url", length = 500) private String slideUrl;
    @Column(name = "report_url", length = 500) private String reportUrl;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "submitted_by", nullable = false)
    private User submittedBy;
    @CreationTimestamp @Column(name = "submitted_at", updatable = false)
    private LocalDateTime submittedAt;
    @Column(name = "change_note") private String changeNote;
}
