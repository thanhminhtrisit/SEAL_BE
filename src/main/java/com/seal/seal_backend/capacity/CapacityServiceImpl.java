package com.seal.seal_backend.capacity;

import com.seal.seal_backend.domain.entity.Event;
import com.seal.seal_backend.domain.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CapacityServiceImpl implements CapacityService {

    private static final String KEY_MIN_TEAM_SIZE        = "TEAM_MIN_SIZE";
    private static final String KEY_MAX_TEAM_SIZE        = "TEAM_MAX_SIZE";
    private static final String KEY_MAX_TEAMS            = "MAX_TEAMS_PER_EVENT";
    private static final String KEY_MAX_PARTICIPANTS     = "MAX_PARTICIPANTS_PER_EVENT";
    private static final String KEY_MAX_TEAMS_PER_MENTOR = "MAX_TEAMS_PER_MENTOR";

    private static final int DEFAULT_MIN_TEAM_SIZE        = 3;
    private static final int DEFAULT_MAX_TEAM_SIZE        = 5;
    private static final int DEFAULT_MAX_TEAMS            = 50;
    private static final int DEFAULT_MAX_PARTICIPANTS     = 300;
    private static final int DEFAULT_MAX_TEAMS_PER_MENTOR = 5;

    private final SystemConfigRepository systemConfigRepository;

    @Override
    public int effectiveMinTeamSize(Event event) {
        return configOrDefault(KEY_MIN_TEAM_SIZE, DEFAULT_MIN_TEAM_SIZE);
    }

    @Override
    public int effectiveMaxTeamSize(Event event) {
        if (event.getMaxTeamSize() != null) return event.getMaxTeamSize();
        return configOrDefault(KEY_MAX_TEAM_SIZE, DEFAULT_MAX_TEAM_SIZE);
    }

    @Override
    public int effectiveMaxTeams(Event event) {
        if (event.getMaxTeams() != null) return event.getMaxTeams();
        return configOrDefault(KEY_MAX_TEAMS, DEFAULT_MAX_TEAMS);
    }

    @Override
    public int effectiveMaxParticipants(Event event) {
        if (event.getMaxParticipants() != null) return event.getMaxParticipants();
        return configOrDefault(KEY_MAX_PARTICIPANTS, DEFAULT_MAX_PARTICIPANTS);
    }

    @Override
    public int effectiveMaxTeamsPerMentor(Event event) {
        if (event.getMaxTeamsPerMentor() != null) return event.getMaxTeamsPerMentor();
        return configOrDefault(KEY_MAX_TEAMS_PER_MENTOR, DEFAULT_MAX_TEAMS_PER_MENTOR);
    }

    private int configOrDefault(String key, int fallback) {
        return systemConfigRepository.findByConfigKey(key)
                .map(cfg -> {
                    try { return Integer.parseInt(cfg.getConfigValue().trim()); }
                    catch (NumberFormatException e) { return fallback; }
                })
                .orElse(fallback);
    }
}
