package com.seal.seal_backend.award.dto.response;

import com.seal.seal_backend.domain.enums.AwardType;

import java.time.LocalDateTime;

public record AwardResponse(
        Long awardId,
        Long eventId,
        Long teamId,
        String teamName,
        AwardType awardType,
        String description,
        Long awardedBy,
        LocalDateTime awardedAt
) {
}