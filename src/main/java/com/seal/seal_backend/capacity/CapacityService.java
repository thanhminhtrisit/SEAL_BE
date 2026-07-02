package com.seal.seal_backend.capacity;

import com.seal.seal_backend.domain.entity.Event;

public interface CapacityService {
    int effectiveMinTeamSize(Event event);
    int effectiveMaxTeamSize(Event event);
    int effectiveMaxTeams(Event event);
    int effectiveMaxParticipants(Event event);
    int effectiveMaxTeamsPerMentor(Event event);
}
