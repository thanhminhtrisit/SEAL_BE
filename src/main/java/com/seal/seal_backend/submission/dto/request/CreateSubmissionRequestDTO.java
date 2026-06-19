package com.seal.seal_backend.submission.dto.request;

import lombok.Data;

@Data
public class CreateSubmissionRequestDTO {
    private Long teamId;
    private Long roundId;

    private String repoUrl;
    private String demoUrl;
    private String slideUrl;
    private String reportUrl;

    private String changeNote;
}
