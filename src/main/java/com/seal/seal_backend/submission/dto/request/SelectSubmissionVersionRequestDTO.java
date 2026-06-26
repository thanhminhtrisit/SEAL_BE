package com.seal.seal_backend.submission.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SelectSubmissionVersionRequestDTO {

    @NotNull(message = "Version id is required")
    private Long versionId;
}
