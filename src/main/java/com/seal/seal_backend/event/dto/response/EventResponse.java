package com.seal.seal_backend.event.dto.response;

import com.seal.seal_backend.domain.entity.Event;
import com.seal.seal_backend.domain.entity.TermPlan;
import com.seal.seal_backend.domain.enums.EventStatus;
import com.seal.seal_backend.domain.enums.EventType;
import com.seal.seal_backend.domain.enums.TermType;
import java.time.LocalDateTime;

public record EventResponse(
        Long id,
        String name,
        String slug,
        EventType eventType,
        Long disciplineId,
        String disciplineName,
        Long termPlanId,
        TermType termPlanTerm,
        Integer termPlanYear,
        String termPlanLabel,
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
        TermPlan tp = e.getTermPlan();
        TermType term = tp != null ? tp.getTerm() : null;
        Integer year = tp != null ? tp.getYear() : null;
        // Human-readable label for FE, e.g. "FALL 2024" (falls back to just the term when year is absent).
        String label = null;
        if (term != null) {
            label = year != null ? term.name() + " " + year : term.name();
        }
        return new EventResponse(
                e.getId(), e.getName(), e.getSlug(), e.getEventType(),
                e.getDiscipline().getId(), e.getDiscipline().getName(),
                tp != null ? tp.getId() : null,
                term, year, label,
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
