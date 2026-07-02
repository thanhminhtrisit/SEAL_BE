package com.seal.seal_backend.scoring.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StartEvaluationRequest {

    @NotNull(message = "Submission id is required")
    private Long submissionId;
}
