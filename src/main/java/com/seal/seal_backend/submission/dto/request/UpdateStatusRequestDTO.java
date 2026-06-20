package com.seal.seal_backend.submission.dto.request;

import com.seal.seal_backend.domain.enums.SubmissionStatus;
import lombok.Data;

@Data
public class UpdateStatusRequestDTO {
    private SubmissionStatus status;
}
