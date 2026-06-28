package com.seal.seal_backend.submission.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SubmissionMyRoundOverviewDTO {
    private Long roundId;
    private String roundName;
    private Integer orderNumber;
    private String status;
    private LocalDateTime submissionDeadline;
    private SubmissionDetailResponseDTO submission;
}
