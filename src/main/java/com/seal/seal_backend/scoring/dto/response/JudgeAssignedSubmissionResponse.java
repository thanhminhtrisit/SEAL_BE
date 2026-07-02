package com.seal.seal_backend.scoring.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class JudgeAssignedSubmissionResponse {

    private Long submissionId;
    private Long teamId;
    private String teamName;
    private Long categoryId;
    private String categoryName;
    private Long roundId;
    private String roundName;
    private Long eventId;
    private String eventName;
    private Integer attemptNumber;
    private LocalDateTime submittedAt;

    private String repoUrl;
    private String demoUrl;
    private String slideUrl;
    private String reportUrl;

    private Long evaluationId;
    private String evaluationStatus;

    private Integer scoredCriteriaCount;
    private Integer totalCriteriaCount;
}
