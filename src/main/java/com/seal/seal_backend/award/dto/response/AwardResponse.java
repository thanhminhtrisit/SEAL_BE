package com.seal.seal_backend.award.dto.response;

import com.seal.seal_backend.domain.enums.AwardType;

import java.time.LocalDateTime;

public record AwardResponse(
        Long awardId,
        Long eventId,
        String eventName,
        Long teamId,
        String teamName,
        String categoryName,
        AwardType awardType,
        String description,
        Long awardedBy,
        LocalDateTime awardedAt
) {
}