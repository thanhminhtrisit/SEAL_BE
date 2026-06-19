package com.seal.seal_backend.submission.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SubmissionResponseDTO {
    private Long submissionId;
    private Long teamId;
    private Long roundId;

    private Long currentVersionId;
    private Integer versionNumber;

    private String repoUrl;
    private String status;

    private LocalDateTime submittedAt;
}
