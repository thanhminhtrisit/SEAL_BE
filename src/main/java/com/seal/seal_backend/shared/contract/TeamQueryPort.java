package com.seal.seal_backend.shared.contract;

import com.seal.seal_backend.shared.contract.dto.TeamView;
import java.util.Optional;

/** IMPLEMENTED BY: team module (M1). CONSUMED BY: submission (M2), ranking (M3), award (M3). */
public interface TeamQueryPort {
    Optional<TeamView> findTeam(Long teamId);
    boolean isTeamActive(Long teamId);
    /** BR-TEAM-02: a user may belong to at most one team per event. */
    boolean userHasTeamInEvent(Long userId, Long eventId);
}
