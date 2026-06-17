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
        RoundStatus status,
        Integer promotionTopN,
        boolean finalRound,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static RoundResponse from(Round r) {
        return new RoundResponse(
                r.getId(), r.getEvent().getId(), r.getName(), r.getOrderNumber(),
                r.getSubmissionDeadline(), r.getStatus(),
                r.getPromotionTopN(), r.getIsFinalRound(),
                r.getCreatedAt(), r.getUpdatedAt()
        );
    }
}
