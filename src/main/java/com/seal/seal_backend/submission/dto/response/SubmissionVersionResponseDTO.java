package com.seal.seal_backend.submission.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SubmissionVersionResponseDTO {

    private Long versionId;
    private Integer versionNumber;

    private String repoUrl;
    private String demoUrl;
    private String slideUrl;
    private String reportUrl;

    private String changeNote;

    private Long submittedBy;
    private LocalDateTime submittedAt;
}
