package com.seal.seal_backend.team.dto.response;

import com.seal.seal_backend.domain.entity.Team;
import com.seal.seal_backend.domain.enums.TeamStatus;

public record TeamSummaryResponse(
        Long id,
        Long eventId,
        Long categoryId,
        String categoryName,
        String name,
        TeamStatus status
) {
    public static TeamSummaryResponse from(Team t) {
        return new TeamSummaryResponse(
                t.getId(),
                t.getEvent().getId(),
                t.getCategory().getId(),
                t.getCategory().getName(),
                t.getName(),
                t.getStatus());
    }
}
