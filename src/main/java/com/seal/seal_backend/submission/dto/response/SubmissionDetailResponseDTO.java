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

    private Long currentVersionId;
    private SubmissionVersionResponseDTO currentVersion;

    private String status;

    private LocalDateTime submittedAt;
    private LocalDateTime lastUpdatedAt;
}
