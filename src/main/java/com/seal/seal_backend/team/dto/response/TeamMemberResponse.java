package com.seal.seal_backend.team.dto.response;

import com.seal.seal_backend.domain.entity.TeamMember;
import com.seal.seal_backend.domain.enums.TeamMemberRole;
import com.seal.seal_backend.domain.enums.TeamMemberStatus;
import java.time.LocalDateTime;

public record TeamMemberResponse(
        Long userId,
        String fullName,
        String email,
        TeamMemberRole role,
        TeamMemberStatus status,
        LocalDateTime joinedAt
) {
    public static TeamMemberResponse from(TeamMember m) {
        return new TeamMemberResponse(
                m.getUser().getId(),
                m.getUser().getFullName(),
                m.getUser().getEmail(),
                m.getMemberRole(),
                m.getStatus(),
                m.getJoinedAt());
    }
}
