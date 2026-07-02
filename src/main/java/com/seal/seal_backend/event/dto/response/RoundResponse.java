package com.seal.seal_backend.event.dto.response;

import com.seal.seal_backend.domain.entity.Round;
import com.seal.seal_backend.domain.enums.RoundStatus;
import java.time.LocalDateTime;

public record RoundResponse(
        Long id,
        Long eventId,
        String name,
        Integer orderNumber,
        LocalDateTime submissionDeadline,
        LocalDateTime scoringDeadline,
        RoundStatus status,
        Integer promotionTopN,
        boolean finalRound,
        boolean requiresRepo,
        boolean requiresDemo,
        boolean requiresSlide,
        boolean requiresReport,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static RoundResponse from(Round r) {
        return new RoundResponse(
                r.getId(), r.getEvent().getId(), r.getName(), r.getOrderNumber(),
                r.getSubmissionDeadline(), r.getScoringDeadline(), r.getStatus(),
                r.getPromotionTopN(), r.getIsFinalRound(),
                Boolean.TRUE.equals(r.getRequiresRepo()),
                Boolean.TRUE.equals(r.getRequiresDemo()),
                Boolean.TRUE.equals(r.getRequiresSlide()),
                Boolean.TRUE.equals(r.getRequiresReport()),
                r.getCreatedAt(), r.getUpdatedAt()
        );
    }
}
