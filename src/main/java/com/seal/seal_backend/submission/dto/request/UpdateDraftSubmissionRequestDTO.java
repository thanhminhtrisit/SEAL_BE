package com.seal.seal_backend.submission.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateDraftSubmissionRequestDTO {
    @Size(max = 500, message = "Repository URL must be at most 500 characters")
    private String repoUrl;

    @Size(max = 500, message = "Demo URL must be at most 500 characters")
    private String demoUrl;

    @Size(max = 500, message = "Slide URL must be at most 500 characters")
    private String slideUrl;

    @Size(max = 500, message = "Report URL must be at most 500 characters")
    private String reportUrl;

    @Size(max = 255, message = "Change note must be at most 255 characters")
    private String changeNote;
}
