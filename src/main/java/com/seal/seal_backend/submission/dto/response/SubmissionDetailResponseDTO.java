package com.seal.seal_backend.submission.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SubmissionDetailResponseDTO {

    private Long submissionId;

    private Long teamId;
    private Long roundId;

    private Long currentVersionId;

    private String status;

    private LocalDateTime submittedAt;
    private LocalDateTime lastUpdatedAt;
}
