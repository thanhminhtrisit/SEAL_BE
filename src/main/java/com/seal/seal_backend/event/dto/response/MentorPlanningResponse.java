package com.seal.seal_backend.event.dto.response;

public record MentorPlanningResponse(
        Long eventId,
        long activeTeams,
        int maxTeamsPerMentor,
        int mentorsNeeded,
        long currentMentors,
        int gap
) {}
