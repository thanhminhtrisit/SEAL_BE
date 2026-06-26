package com.seal.seal_backend.submission.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class SubmissionMyTeamOverviewDTO {
    private Long teamId;
    private String teamName;
    private Long eventId;
    private String eventName;
    private Long categoryId;
    private String categoryName;
    private String memberRole;
    private List<SubmissionMyRoundOverviewDTO> rounds;
}
