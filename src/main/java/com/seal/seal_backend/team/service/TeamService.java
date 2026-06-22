package com.seal.seal_backend.team.service;

import com.seal.seal_backend.team.dto.request.*;
import com.seal.seal_backend.team.dto.response.*;
import java.util.List;

public interface TeamService {

    // FR-TEAM-01: create team, creator becomes LEADER
    TeamResponse createTeam(CreateTeamRequest req, Long creatorId);

    // Read
    TeamResponse getTeam(Long teamId);
    List<TeamSummaryResponse> listTeamsByEvent(Long eventId);

    // FR-TEAM-03: invite member by email (BR-TEAM-06: no duplicate in same event)
    InvitationResponse inviteMember(Long teamId, InviteMemberRequest req, Long inviterId);
    // FIX 1: authz — only COORDINATOR/SUPER_COORDINATOR or active team member
    List<InvitationResponse> listInvitations(Long teamId, Long requesterId, String requesterRoleCode);
    // FIX 2: invitations sent to the caller's email (PENDING only)
    List<MyInvitationResponse> listMyInvitations(String email);

    // Accept / decline invitation (invitee calls these)
    TeamResponse acceptInvitation(Long invitationId, Long userId);
    InvitationResponse declineInvitation(Long invitationId, Long userId);

    // FR-TEAM-04: change/set category (BR-TEAM-04: within reg window)
    TeamResponse registerCategory(Long teamId, RegisterTeamCategoryRequest req, Long requesterId);

    // FR-TEAM-05: coordinator approve/reject (BR-TEAM-01: size check on approve)
    TeamResponse reviewTeam(Long teamId, ApproveTeamRequest req, Long coordinatorId, String ip);

    // FR-TEAM-07: remove member (leader or self); warns but allows if size drops below min
    TeamResponse removeMember(Long teamId, Long targetUserId, Long requesterId);
}
