package com.seal.seal_backend.submission.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class SubmissionMyOverviewResponseDTO {
    private List<SubmissionMyTeamOverviewDTO> teams;
}
