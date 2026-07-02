package com.seal.seal_backend.submission.dto.response;

import com.seal.seal_backend.domain.entity.Round;

public record SubmissionRequirementsDTO(
        boolean requiresRepo,
        boolean requiresDemo,
        boolean requiresSlide,
        boolean requiresReport
) {
    public static SubmissionRequirementsDTO from(Round round) {
        return new SubmissionRequirementsDTO(
                Boolean.TRUE.equals(round.getRequiresRepo()),
                Boolean.TRUE.equals(round.getRequiresDemo()),
                Boolean.TRUE.equals(round.getRequiresSlide()),
                Boolean.TRUE.equals(round.getRequiresReport())
        );
    }
}
