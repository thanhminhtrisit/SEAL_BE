package com.seal.seal_backend.team.service;

import com.seal.seal_backend.shared.contract.TeamQueryPort;
import com.seal.seal_backend.shared.contract.dto.TeamView;
import org.springframework.stereotype.Service;
import java.util.Optional;

/** OWNER: M1. Stub of TeamQueryPort. */
@Service
public class TeamQueryAdapter implements TeamQueryPort {
    @Override public Optional<TeamView> findTeam(Long teamId) { return Optional.empty(); }
    @Override public boolean isTeamActive(Long teamId) { return false; }
    @Override public boolean userHasTeamInEvent(Long userId, Long eventId) { return false; }
}
