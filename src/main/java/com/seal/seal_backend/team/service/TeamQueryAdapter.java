package com.seal.seal_backend.team.service;

import com.seal.seal_backend.domain.enums.TeamMemberStatus;
import com.seal.seal_backend.domain.enums.TeamStatus;
import com.seal.seal_backend.domain.repository.TeamMemberRepository;
import com.seal.seal_backend.domain.repository.TeamRepository;
import com.seal.seal_backend.shared.contract.TeamQueryPort;
import com.seal.seal_backend.shared.contract.dto.TeamView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** OWNER: M1. Real implementation consumed by M2 (submission/scoring) and M3 (ranking/award). */
@Service
@RequiredArgsConstructor
public class TeamQueryAdapter implements TeamQueryPort {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<TeamView> findTeam(Long teamId) {
        return teamRepository.findById(teamId)
                .map(t -> new TeamView(
                        t.getId(),
                        t.getEvent().getId(),
                        t.getCategory().getId(),
                        t.getLeader().getId(),
                        t.getName(),
                        t.getStatus()));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTeamActive(Long teamId) {
        return teamRepository.findById(teamId)
                .map(t -> t.getStatus() == TeamStatus.ACTIVE || t.getStatus() == TeamStatus.APPROVED)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean userHasTeamInEvent(Long userId, Long eventId) {
        return teamRepository.existsActiveMemberByUserIdAndEventId(userId, eventId);
    }
}
