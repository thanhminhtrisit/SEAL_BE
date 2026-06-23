package com.seal.seal_backend.team.dto.response;

import com.seal.seal_backend.domain.entity.TeamInvitation;
import java.time.LocalDateTime;

public record MyInvitationResponse(
        Long invitationId,
        Long teamId,
        String teamName,
        Long eventId,
        String eventName,
        String invitedByName,
        LocalDateTime expiresAt
) {
    public static MyInvitationResponse from(TeamInvitation inv) {
        return new MyInvitationResponse(
                inv.getId(),
                inv.getTeam().getId(),
                inv.getTeam().getName(),
                inv.getTeam().getEvent().getId(),
                inv.getTeam().getEvent().getName(),
                inv.getInvitedBy().getFullName(),
                inv.getExpiresAt());
    }
}
