package com.seal.seal_backend.submission.dto.request;

import lombok.Data;

@Data
public class CreateVersionRequestDTO {

    private String repoUrl;
    private String demoUrl;
    private String slideUrl;
    private String reportUrl;
    private String changeNote;
}