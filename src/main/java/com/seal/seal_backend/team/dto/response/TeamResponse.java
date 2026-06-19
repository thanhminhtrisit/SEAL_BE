package com.seal.seal_backend.team.dto.response;

import com.seal.seal_backend.domain.entity.Team;
import com.seal.seal_backend.domain.enums.TeamStatus;
import java.time.LocalDateTime;
import java.util.List;

public record TeamResponse(
        Long id,
        Long eventId,
        Long categoryId,
        String categoryName,
        Long leaderId,
        String leaderName,
        String name,
        String description,
        TeamStatus status,
        String rejectionReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<TeamMemberResponse> members
) {
    public static TeamResponse from(Team t, List<TeamMemberResponse> members) {
        return new TeamResponse(
                t.getId(),
                t.getEvent().getId(),
                t.getCategory().getId(),
                t.getCategory().getName(),
                t.getLeader().getId(),
                t.getLeader().getFullName(),
                t.getName(),
                t.getDescription(),
                t.getStatus(),
                t.getRejectionReason(),
                t.getCreatedAt(),
                t.getUpdatedAt(),
                members);
    }
}
