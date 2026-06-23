package com.seal.seal_backend.scoring.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StartEvaluationRequest {

    private Long judgeAssignmentId;

    @NotNull(message = "Judge id is required")
    private Long judgeId;

    @NotNull(message = "Submission version id is required")
    private Long submissionVersionId;

    @NotNull(message = "Round id is required")
    private Long roundId;
}
