package com.seal.seal_backend.event.dto.response;

import com.seal.seal_backend.domain.entity.Event;
import com.seal.seal_backend.domain.enums.EventStatus;
import com.seal.seal_backend.domain.enums.EventType;
import java.time.LocalDateTime;

public record EventResponse(
        Long id,
        String name,
        String slug,
        EventType eventType,
        Long disciplineId,
        String disciplineName,
        Long termPlanId,
        String description,
        LocalDateTime registrationStart,
        LocalDateTime registrationEnd,
        EventStatus status,
        Long ownerCoordinatorId,
        Integer maxTeamSize,
        Integer maxTeams,
        Integer maxParticipants,
        Integer maxTeamsPerMentor,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static EventResponse from(Event e) {
        return new EventResponse(
                e.getId(), e.getName(), e.getSlug(), e.getEventType(),
                e.getDiscipline().getId(), e.getDiscipline().getName(),
                e.getTermPlan().getId(),
                e.getDescription(),
                e.getRegistrationStart(), e.getRegistrationEnd(),
                e.getStatus(),
                e.getOwnerCoordinator().getId(),
                e.getMaxTeamSize(), e.getMaxTeams(),
                e.getMaxParticipants(), e.getMaxTeamsPerMentor(),
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
