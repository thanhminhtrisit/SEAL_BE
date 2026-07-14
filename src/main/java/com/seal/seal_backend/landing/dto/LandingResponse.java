package com.seal.seal_backend.landing.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Public landing-page payload — aggregate stats + the events currently open for registration. */
public record LandingResponse(Stats stats, List<EventCard> events) {

    public record Stats(long openEvents, long registeredTeams, long categories, long judges) {}

    public record EventCard(
            Long id,
            String name,
            String disciplineName,
            String eventType,
            LocalDateTime registrationStart,
            LocalDateTime registrationEnd,
            Integer maxTeams,
            long registeredTeams,
            List<String> categories) {}
}
