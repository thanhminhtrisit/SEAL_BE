package com.seal.seal_backend.capacity;

import com.seal.seal_backend.domain.entity.Event;
import com.seal.seal_backend.domain.entity.SystemConfig;
import com.seal.seal_backend.domain.repository.SystemConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CapacityServiceTest {

    @Mock SystemConfigRepository systemConfigRepository;
    @InjectMocks CapacityServiceImpl service;

    private Event event;

    @BeforeEach
    void setup() {
        event = new Event();
    }

    private SystemConfig config(String key, String value) {
        SystemConfig c = new SystemConfig();
        c.setConfigKey(key);
        c.setConfigValue(value);
        return c;
    }

    @Nested
    class MaxTeamSize {

        @Test
        void eventOverride_wins_over_config() {
            event.setMaxTeamSize(8);
            assertThat(service.effectiveMaxTeamSize(event)).isEqualTo(8);
        }

        @Test
        void noOverride_usesConfig() {
            when(systemConfigRepository.findByConfigKey("TEAM_MAX_SIZE"))
                    .thenReturn(Optional.of(config("TEAM_MAX_SIZE", "7")));

            assertThat(service.effectiveMaxTeamSize(event)).isEqualTo(7);
        }

        @Test
        void noOverride_noConfig_usesHardcoded() {
            when(systemConfigRepository.findByConfigKey("TEAM_MAX_SIZE"))
                    .thenReturn(Optional.empty());

            assertThat(service.effectiveMaxTeamSize(event)).isEqualTo(5);
        }
    }

    @Nested
    class MaxTeams {

        @Test
        void eventOverride_wins() {
            event.setMaxTeams(20);
            assertThat(service.effectiveMaxTeams(event)).isEqualTo(20);
        }

        @Test
        void noOverride_noConfig_usesHardcoded() {
            when(systemConfigRepository.findByConfigKey("MAX_TEAMS_PER_EVENT"))
                    .thenReturn(Optional.empty());

            assertThat(service.effectiveMaxTeams(event)).isEqualTo(50);
        }

        @Test
        void noOverride_configPresent_usesConfig() {
            when(systemConfigRepository.findByConfigKey("MAX_TEAMS_PER_EVENT"))
                    .thenReturn(Optional.of(config("MAX_TEAMS_PER_EVENT", "30")));

            assertThat(service.effectiveMaxTeams(event)).isEqualTo(30);
        }
    }

    @Nested
    class MaxParticipants {

        @Test
        void eventOverride_wins() {
            event.setMaxParticipants(100);
            assertThat(service.effectiveMaxParticipants(event)).isEqualTo(100);
        }

        @Test
        void noOverride_noConfig_usesHardcoded() {
            when(systemConfigRepository.findByConfigKey("MAX_PARTICIPANTS_PER_EVENT"))
                    .thenReturn(Optional.empty());

            assertThat(service.effectiveMaxParticipants(event)).isEqualTo(300);
        }
    }

    @Nested
    class MaxTeamsPerMentor {

        @Test
        void eventOverride_wins() {
            event.setMaxTeamsPerMentor(3);
            assertThat(service.effectiveMaxTeamsPerMentor(event)).isEqualTo(3);
        }

        @Test
        void noOverride_noConfig_usesHardcoded() {
            when(systemConfigRepository.findByConfigKey("MAX_TEAMS_PER_MENTOR"))
                    .thenReturn(Optional.empty());

            assertThat(service.effectiveMaxTeamsPerMentor(event)).isEqualTo(5);
        }
    }

    @Nested
    class MinTeamSize {

        @Test
        void noConfig_usesHardcoded() {
            when(systemConfigRepository.findByConfigKey("TEAM_MIN_SIZE"))
                    .thenReturn(Optional.empty());

            assertThat(service.effectiveMinTeamSize(event)).isEqualTo(3);
        }

        @Test
        void configPresent_usesConfig() {
            when(systemConfigRepository.findByConfigKey("TEAM_MIN_SIZE"))
                    .thenReturn(Optional.of(config("TEAM_MIN_SIZE", "2")));

            assertThat(service.effectiveMinTeamSize(event)).isEqualTo(2);
        }

        @Test
        void configValueMalformed_fallsBackToHardcoded() {
            when(systemConfigRepository.findByConfigKey("TEAM_MIN_SIZE"))
                    .thenReturn(Optional.of(config("TEAM_MIN_SIZE", "not-a-number")));

            assertThat(service.effectiveMinTeamSize(event)).isEqualTo(3);
        }
    }

    @Test
    void allMethodsWithNoOverrideAndNoConfig_returnHardcodedDefaults() {
        when(systemConfigRepository.findByConfigKey(anyString())).thenReturn(Optional.empty());

        assertThat(service.effectiveMinTeamSize(event)).isEqualTo(3);
        assertThat(service.effectiveMaxTeamSize(event)).isEqualTo(5);
        assertThat(service.effectiveMaxTeams(event)).isEqualTo(50);
        assertThat(service.effectiveMaxParticipants(event)).isEqualTo(300);
        assertThat(service.effectiveMaxTeamsPerMentor(event)).isEqualTo(5);
    }
}
