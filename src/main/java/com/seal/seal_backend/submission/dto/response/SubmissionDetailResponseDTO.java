package com.seal.seal_backend.submission.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SubmissionDetailResponseDTO {

    private Long submissionId;

    private Long teamId;
    private String teamName;
    private Long roundId;
    private String roundName;
    private Long eventId;
    private String eventName;
    private Long categoryId;
    private String categoryName;

    private Integer attemptNumber;
    private String repoUrl;
    private String demoUrl;
    private String slideUrl;
    private String reportUrl;
    private String changeNote;
    private Long submittedBy;

    private String status;

    private LocalDateTime submittedAt;
    private LocalDateTime lastUpdatedAt;
}
