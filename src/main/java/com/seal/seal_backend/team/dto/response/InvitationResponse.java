package com.seal.seal_backend.team.dto.response;

import com.seal.seal_backend.domain.entity.TeamInvitation;
import com.seal.seal_backend.domain.enums.InvitationStatus;
import java.time.LocalDateTime;

public record InvitationResponse(
        Long id,
        Long teamId,
        String email,
        Long invitedUserId,
        InvitationStatus status,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {
    public static InvitationResponse from(TeamInvitation inv) {
        return new InvitationResponse(
                inv.getId(),
                inv.getTeam().getId(),
                inv.getEmail(),
                inv.getInvitedUser() != null ? inv.getInvitedUser().getId() : null,
                inv.getStatus(),
                inv.getExpiresAt(),
                inv.getCreatedAt());
    }
}
