package com.seal.seal_backend.team.service.impl;

import com.seal.seal_backend.capacity.CapacityService;
import com.seal.seal_backend.common.audit.AuditAction;
import com.seal.seal_backend.common.audit.AuditPublisher;
import com.seal.seal_backend.common.exception.BusinessRuleException;
import com.seal.seal_backend.common.exception.ForbiddenActionException;
import com.seal.seal_backend.common.exception.ResourceNotFoundException;
import com.seal.seal_backend.domain.entity.*;
import com.seal.seal_backend.domain.enums.*;
import com.seal.seal_backend.domain.repository.*;
import com.seal.seal_backend.team.dto.request.*;
import com.seal.seal_backend.team.dto.response.*;
import com.seal.seal_backend.team.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamServiceImpl implements TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamInvitationRepository teamInvitationRepository;
    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CapacityService capacityService;
    private final AuditPublisher auditPublisher;

    // ─── FR-TEAM-01: Create team ──────────────────────────────────────────────

    @Override
    @Transactional
    public TeamResponse createTeam(CreateTeamRequest req, Long creatorId) {
        Event event = findEvent(req.eventId());
        Category category = findCategory(req.categoryId(), req.eventId());

        // BR-TEAM-04: event OPEN + within registration window
        validateRegistrationOpen(event);

        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + creatorId));

        // BR-TEAM-02: creator may not already be a member of any team in this event
        if (teamRepository.existsActiveMemberByUserIdAndEventId(creatorId, req.eventId())) {
            throw new BusinessRuleException("BR-TEAM-02",
                    "User is already a member of another team in this event");
        }

        // FIX #4: duplicate team name is NOT BR-TEAM-02 — use a distinct code
        if (teamRepository.existsByEventIdAndName(req.eventId(), req.name())) {
            throw new BusinessRuleException("TEAM-NAME-CONFLICT",
                    "Team name '" + req.name() + "' already exists in this event");
        }

        // BR-CAP-01: event max teams
        long activeTeams = teamRepository.countActiveTeamsByEventId(req.eventId());
        if (activeTeams >= capacityService.effectiveMaxTeams(event)) {
            throw new BusinessRuleException("BR-CAP-01", "Đã đạt số nhóm tối đa của sự kiện");
        }
        // BR-CAP-02: event max participants
        long currentParticipants = teamMemberRepository.countDistinctParticipantsByEventId(req.eventId());
        if (currentParticipants >= capacityService.effectiveMaxParticipants(event)) {
            throw new BusinessRuleException("BR-CAP-02", "Đã đạt số người tham gia tối đa của sự kiện");
        }

        Team team = new Team();
        team.setEvent(event);
        team.setCategory(category);
        team.setLeader(creator);
        team.setName(req.name());
        team.setDescription(req.description());
        team.setStatus(TeamStatus.REGISTERED);
        team = teamRepository.save(team);

        // BR-TEAM-05: creator is the unique LEADER
        TeamMember leader = new TeamMember();
        leader.setTeam(team);
        leader.setUser(creator);
        leader.setMemberRole(TeamMemberRole.LEADER);
        leader.setStatus(TeamMemberStatus.ACTIVE);
        teamMemberRepository.save(leader);

        List<TeamMemberResponse> members = teamMemberRepository.findByTeamId(team.getId())
                .stream().map(TeamMemberResponse::from).toList();
        return TeamResponse.from(team, members);
    }

    // ─── Read ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public TeamResponse getTeam(Long teamId) {
        Team team = findTeam(teamId);
        List<TeamMemberResponse> members = teamMemberRepository.findByTeamId(teamId)
                .stream().map(TeamMemberResponse::from).toList();
        return TeamResponse.from(team, members);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamSummaryResponse> listTeamsByEvent(Long eventId, TeamStatus status) {
        findEvent(eventId);
        List<Team> teams = status == null
                ? teamRepository.findByEventIdOrderByCreatedAtAsc(eventId)
                : teamRepository.findByEventIdAndStatusOrderByCreatedAtAsc(eventId, status);
        return teams.stream()
                .map(t -> TeamSummaryResponse.from(t, teamMemberRepository.countActiveByTeamId(t.getId())))
                .toList();
    }

    // ─── FR-TEAM-03: Invite member ────────────────────────────────────────────

    @Override
    @Transactional
    public InvitationResponse inviteMember(Long teamId, InviteMemberRequest req, Long inviterId) {
        Team team = findTeam(teamId);
        User inviter = userRepository.findById(inviterId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + inviterId));

        // Only LEADER may invite
        requireActiveLeader(teamId, inviterId, "send invitations");

        // BR-TEAM-08: roster changes only while team is awaiting review
        if (team.getStatus() != TeamStatus.REGISTERED) {
            throw new BusinessRuleException("BR-TEAM-08",
                    "Invitations are only allowed while the team is REGISTERED (current: "
                    + team.getStatus() + ")");
        }
        // BR-TEAM-04: joining is part of registration — event must still be open
        validateRegistrationOpen(team.getEvent());

        // BR-TEAM-06 + DB UNIQUE(team_id,email): one invitation row per (team,email).
        // PENDING blocks a duplicate; DECLINED/EXPIRED/CANCELLED rows are RE-USED for re-invites.
        TeamInvitation existing = teamInvitationRepository
                .findByTeamIdAndEmail(teamId, req.email()).orElse(null);
        if (existing != null && existing.getStatus() == InvitationStatus.PENDING) {
            throw new BusinessRuleException("BR-TEAM-06",
                    "An invitation has already been sent to " + req.email() + " for this team");
        }

        // BR-TEAM-06: invitee already active in a running team for this event
        // (also covers ACCEPTED invitations whose member is still active in this team)
        userRepository.findByEmail(req.email()).ifPresent(invitee -> {
            if (teamRepository.existsActiveMemberByUserIdAndEventId(invitee.getId(), team.getEvent().getId())) {
                throw new BusinessRuleException("BR-TEAM-06",
                        "User " + req.email() + " already belongs to a team in this event");
            }
        });

        // BR-CAP-03: team must not already be at capacity
        long currentSize = teamMemberRepository.countActiveByTeamId(teamId);
        if (currentSize >= capacityService.effectiveMaxTeamSize(team.getEvent())) {
            throw new BusinessRuleException("BR-CAP-03", "Nhóm đã đủ sĩ số tối đa");
        }

        TeamInvitation inv = existing != null ? existing : new TeamInvitation();
        inv.setTeam(team);
        inv.setEmail(req.email());
        inv.setInvitedBy(inviter);
        inv.setStatus(InvitationStatus.PENDING);
        inv.setAcceptedAt(null);
        userRepository.findByEmail(req.email()).ifPresent(inv::setInvitedUser);
        inv.setExpiresAt(LocalDateTime.now().plusDays(7));

        return InvitationResponse.from(teamInvitationRepository.save(inv));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvitationResponse> listInvitations(Long teamId, Long requesterId, String requesterRoleCode) {
        findTeam(teamId);
        boolean isCoordinatorLevel = "COORDINATOR".equals(requesterRoleCode)
                || "SUPER_COORDINATOR".equals(requesterRoleCode);
        if (!isCoordinatorLevel) {
            boolean isActiveMember = teamMemberRepository.findByTeamId(teamId).stream()
                    .anyMatch(m -> m.getUser().getId().equals(requesterId)
                            && m.getStatus() == TeamMemberStatus.ACTIVE);
            if (!isActiveMember) {
                throw new ForbiddenActionException(
                        "Access denied: only team members or coordinators may view invitations");
            }
        }
        return teamInvitationRepository.findByTeamIdOrderByCreatedAtDesc(teamId)
                .stream().map(InvitationResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MyInvitationResponse> listMyInvitations(String email) {
        return teamInvitationRepository.findByEmailAndStatus(email, InvitationStatus.PENDING)
                .stream().map(MyInvitationResponse::from).toList();
    }

    // ─── FIX #1: Accept / decline invitation ──────────────────────────────────

    @Override
    @Transactional
    public TeamResponse acceptInvitation(Long invitationId, Long userId) {
        TeamInvitation inv = teamInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found: " + invitationId));

        if (inv.getStatus() != InvitationStatus.PENDING) {
            throw new BusinessRuleException("INV-STATUS",
                    "Invitation is no longer pending (status: " + inv.getStatus() + ")");
        }

        // Mark expired if past expiry date
        if (inv.getExpiresAt() != null && LocalDateTime.now().isAfter(inv.getExpiresAt())) {
            inv.setStatus(InvitationStatus.EXPIRED);
            teamInvitationRepository.save(inv);
            throw new BusinessRuleException("INV-EXPIRED", "Invitation has expired");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        // Verify the authenticated user is the intended recipient
        boolean emailMatch = user.getEmail().equals(inv.getEmail());
        boolean idMatch = inv.getInvitedUser() != null && inv.getInvitedUser().getId().equals(userId);
        if (!emailMatch && !idMatch) {
            throw new ForbiddenActionException("You are not the intended recipient of this invitation");
        }

        Team team = inv.getTeam();
        Long eventId = team.getEvent().getId();

        // BR-TEAM-08: roster is frozen once the team has been reviewed — otherwise a
        // late accept silently changes an APPROVED team AFTER the size check at review.
        if (team.getStatus() != TeamStatus.REGISTERED) {
            throw new BusinessRuleException("BR-TEAM-08",
                    "This team can no longer accept members (status: " + team.getStatus() + ")");
        }
        // BR-TEAM-04: joining is part of registration — event must still be open
        validateRegistrationOpen(team.getEvent());

        // BR-TEAM-02: user must not already belong to any team in this event
        if (teamRepository.existsActiveMemberByUserIdAndEventId(userId, eventId)) {
            throw new BusinessRuleException("BR-TEAM-02",
                    "You already belong to another team in this event");
        }

        // BR-CAP-03: team at capacity
        Event teamEvent = team.getEvent();
        long currentSize = teamMemberRepository.countActiveByTeamId(team.getId());
        if (currentSize >= capacityService.effectiveMaxTeamSize(teamEvent)) {
            throw new BusinessRuleException("BR-CAP-03", "Nhóm đã đủ sĩ số tối đa");
        }
        // BR-CAP-02: event participant capacity
        long totalParticipants = teamMemberRepository.countDistinctParticipantsByEventId(eventId);
        if (totalParticipants >= capacityService.effectiveMaxParticipants(teamEvent)) {
            throw new BusinessRuleException("BR-CAP-02", "Đã đạt số người tham gia tối đa của sự kiện");
        }

        // Create OR re-activate the TeamMember — (team_id,user_id) is the composite PK,
        // so a user who LEFT/was REMOVED must reuse the old row, not insert a duplicate.
        TeamMember member = teamMemberRepository.findByTeamId(team.getId()).stream()
                .filter(m -> m.getUser().getId().equals(userId))
                .findFirst()
                .orElseGet(() -> {
                    TeamMember m = new TeamMember();
                    m.setTeam(team);
                    m.setUser(user);
                    return m;
                });
        member.setMemberRole(TeamMemberRole.MEMBER);
        member.setStatus(TeamMemberStatus.ACTIVE);
        member.setLeftAt(null);
        teamMemberRepository.save(member);

        // Accept and link user to invitation
        inv.setStatus(InvitationStatus.ACCEPTED);
        inv.setInvitedUser(user);
        inv.setAcceptedAt(LocalDateTime.now());
        teamInvitationRepository.save(inv);

        List<TeamMemberResponse> members = teamMemberRepository.findByTeamId(team.getId())
                .stream().map(TeamMemberResponse::from).toList();
        return TeamResponse.from(team, members);
    }

    @Override
    @Transactional
    public InvitationResponse declineInvitation(Long invitationId, Long userId) {
        TeamInvitation inv = teamInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found: " + invitationId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        boolean emailMatch = user.getEmail().equals(inv.getEmail());
        boolean idMatch = inv.getInvitedUser() != null && inv.getInvitedUser().getId().equals(userId);
        if (!emailMatch && !idMatch) {
            throw new ForbiddenActionException("You are not the intended recipient of this invitation");
        }

        if (inv.getStatus() != InvitationStatus.PENDING) {
            throw new BusinessRuleException("INV-STATUS",
                    "Invitation is no longer pending (status: " + inv.getStatus() + ")");
        }

        inv.setStatus(InvitationStatus.DECLINED);
        return InvitationResponse.from(teamInvitationRepository.save(inv));
    }

    // ─── FR-TEAM-04: Register / change category ───────────────────────────────

    @Override
    @Transactional
    public TeamResponse registerCategory(Long teamId, RegisterTeamCategoryRequest req, Long requesterId) {
        Team team = findTeam(teamId);
        Event event = team.getEvent();

        // Only leader may change category
        requireActiveLeader(teamId, requesterId, "change the category");

        // FIX #2: REMOVED wrong "one active team per category" block.
        // BR-TEAM-03 means each TEAM has exactly 1 category (enforced by the DB column category_id).
        // Multiple teams are allowed in the same category.
        // Only block: approved/active team cannot change category without re-review.
        if (team.getStatus() == TeamStatus.APPROVED || team.getStatus() == TeamStatus.ACTIVE) {
            throw new BusinessRuleException("BR-TEAM-03",
                    "Cannot change category of an approved/active team without re-registration");
        }

        // BR-TEAM-04: category changes are part of registration — window must be open
        validateRegistrationOpen(event);

        Category newCategory = findCategory(req.categoryId(), event.getId());

        team.setCategory(newCategory);
        team = teamRepository.save(team);

        List<TeamMemberResponse> members = teamMemberRepository.findByTeamId(teamId)
                .stream().map(TeamMemberResponse::from).toList();
        return TeamResponse.from(team, members);
    }

    // ─── FR-TEAM-05: Coordinator approve/reject ───────────────────────────────

    @Override
    @Transactional
    public TeamResponse reviewTeam(Long teamId, ApproveTeamRequest req, Long coordinatorId, String ip) {
        Team team = findTeam(teamId);
        User coordinator = userRepository.findById(coordinatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + coordinatorId));

        if (team.getStatus() != TeamStatus.REGISTERED) {
            throw new BusinessRuleException("BR-TEAM-05",
                    "Only REGISTERED teams can be reviewed; current status: " + team.getStatus());
        }

        String oldJson = "{\"status\":\"" + team.getStatus() + "\"}";

        if (req.approved()) {
            long activeCount = teamMemberRepository.countActiveByTeamId(teamId);
            Event teamEvent = team.getEvent();
            // BR-CAP-04: min size check (uses capacity service, replaces hardcoded 3)
            if (activeCount < capacityService.effectiveMinTeamSize(teamEvent)) {
                throw new BusinessRuleException("BR-CAP-04", "Nhóm chưa đủ sĩ số tối thiểu để duyệt");
            }
            // BR-TEAM-01: max size check
            if (activeCount > capacityService.effectiveMaxTeamSize(teamEvent)) {
                throw new BusinessRuleException("BR-TEAM-01",
                        "Team exceeds maximum size of " + capacityService.effectiveMaxTeamSize(teamEvent));
            }
            team.setStatus(TeamStatus.APPROVED);
            team.setApprovedBy(coordinator);
            team.setApprovedAt(LocalDateTime.now());
        } else {
            team.setStatus(TeamStatus.REJECTED);
            team.setRejectionReason(req.reason());
        }

        team = teamRepository.save(team);
        String newJson = "{\"status\":\"" + team.getStatus() + "\"}";

        // FIX #3: use correct audit action
        AuditAction action = req.approved() ? AuditAction.TEAM_APPROVED : AuditAction.TEAM_REJECTED;
        auditPublisher.log(coordinator, action, "TEAM", teamId, oldJson, newJson, req.reason(), ip);

        List<TeamMemberResponse> members = teamMemberRepository.findByTeamId(teamId)
                .stream().map(TeamMemberResponse::from).toList();
        return TeamResponse.from(team, members);
    }

    // ─── FR-TEAM-07: Remove member ────────────────────────────────────────────

    @Override
    @Transactional
    public TeamResponse removeMember(Long teamId, Long targetUserId, Long requesterId) {
        Team team = findTeam(teamId);

        // BR-TEAM-08: roster is frozen after review — the coordinator approved THIS roster.
        // Leaving a REGISTERED or REJECTED team is fine.
        if (team.getStatus() == TeamStatus.APPROVED || team.getStatus() == TeamStatus.ACTIVE) {
            throw new BusinessRuleException("BR-TEAM-08",
                    "Team roster is locked after approval; ask the coordinator to handle roster changes");
        }

        List<TeamMember> allMembers = teamMemberRepository.findByTeamId(teamId);

        boolean isLeader = allMembers.stream()
                .anyMatch(m -> m.getUser().getId().equals(requesterId)
                        && m.getMemberRole() == TeamMemberRole.LEADER
                        && m.getStatus() == TeamMemberStatus.ACTIVE);
        boolean isSelf = requesterId.equals(targetUserId);

        if (!isLeader && !isSelf) {
            throw new ForbiddenActionException("Only the team leader or the member themselves can remove a member");
        }

        TeamMember target = allMembers.stream()
                .filter(m -> m.getUser().getId().equals(targetUserId)
                        && m.getStatus() == TeamMemberStatus.ACTIVE)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Active member " + targetUserId + " not found in team " + teamId));

        if (target.getMemberRole() == TeamMemberRole.LEADER) {
            throw new BusinessRuleException("BR-TEAM-07",
                    "The team leader cannot be removed; transfer leadership first (PUT /api/teams/{id}/leader)");
        }

        // LEFT = voluntary exit, REMOVED = kicked by leader (schema has both states)
        target.setStatus(isSelf ? TeamMemberStatus.LEFT : TeamMemberStatus.REMOVED);
        target.setLeftAt(LocalDateTime.now());
        teamMemberRepository.save(target);

        List<TeamMemberResponse> members = teamMemberRepository.findByTeamId(teamId)
                .stream().map(TeamMemberResponse::from).toList();
        return TeamResponse.from(team, members);
    }

    // ─── FR-TEAM-06: Update team info ─────────────────────────────────────────

    @Override
    @Transactional
    public TeamResponse updateTeam(Long teamId, UpdateTeamRequest req, Long requesterId) {
        Team team = findTeam(teamId);
        requireActiveLeader(teamId, requesterId, "update the team");

        if (team.getStatus() != TeamStatus.REGISTERED && team.getStatus() != TeamStatus.REJECTED) {
            throw new BusinessRuleException("BR-TEAM-08",
                    "Team info can only be edited before approval (current: " + team.getStatus() + ")");
        }
        if (req.name() != null && !req.name().isBlank() && !req.name().equals(team.getName())) {
            if (teamRepository.existsByEventIdAndName(team.getEvent().getId(), req.name())) {
                throw new BusinessRuleException("TEAM-NAME-CONFLICT",
                        "Team name '" + req.name() + "' already exists in this event");
            }
            team.setName(req.name());
        }
        if (req.description() != null) {
            team.setDescription(req.description());
        }
        return toResponse(teamRepository.save(team));
    }

    // ─── Resubmit after rejection (REJECTED → REGISTERED) ────────────────────

    @Override
    @Transactional
    public TeamResponse resubmitTeam(Long teamId, Long requesterId) {
        Team team = findTeam(teamId);
        requireActiveLeader(teamId, requesterId, "resubmit the team");

        if (team.getStatus() != TeamStatus.REJECTED) {
            throw new BusinessRuleException("BR-TEAM-05",
                    "Only REJECTED teams can be resubmitted (current: " + team.getStatus() + ")");
        }
        // Resubmission is a registration action — window must still be open
        validateRegistrationOpen(team.getEvent());

        team.setStatus(TeamStatus.REGISTERED);
        team.setRejectionReason(null);
        team.setApprovedBy(null);
        team.setApprovedAt(null);
        return toResponse(teamRepository.save(team));
    }

    // ─── BR-TEAM-05: Transfer leadership ──────────────────────────────────────

    @Override
    @Transactional
    public TeamResponse transferLeadership(Long teamId, TransferLeadershipRequest req, Long requesterId) {
        Team team = findTeam(teamId);
        if (team.getStatus() == TeamStatus.WITHDRAWN || team.getStatus() == TeamStatus.DISQUALIFIED) {
            throw new BusinessRuleException("BR-TEAM-08",
                    "Cannot transfer leadership of a " + team.getStatus() + " team");
        }
        if (requesterId.equals(req.newLeaderId())) {
            throw new BusinessRuleException("BR-TEAM-05", "You are already the team leader");
        }

        List<TeamMember> members = teamMemberRepository.findByTeamId(teamId);
        TeamMember current = members.stream()
                .filter(m -> m.getUser().getId().equals(requesterId)
                        && m.getMemberRole() == TeamMemberRole.LEADER
                        && m.getStatus() == TeamMemberStatus.ACTIVE)
                .findFirst()
                .orElseThrow(() -> new ForbiddenActionException(
                        "Only the current team leader can transfer leadership"));
        TeamMember target = members.stream()
                .filter(m -> m.getUser().getId().equals(req.newLeaderId())
                        && m.getStatus() == TeamMemberStatus.ACTIVE)
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("BR-TEAM-05",
                        "New leader must be an ACTIVE member of this team"));

        current.setMemberRole(TeamMemberRole.MEMBER);
        target.setMemberRole(TeamMemberRole.LEADER);
        teamMemberRepository.save(current);
        teamMemberRepository.save(target);

        team.setLeader(target.getUser());
        return toResponse(teamRepository.save(team));
    }

    // ─── Withdraw team (→ WITHDRAWN, members freed by the active-member query) ─

    @Override
    @Transactional
    public TeamResponse withdrawTeam(Long teamId, Long requesterId) {
        Team team = findTeam(teamId);
        requireActiveLeader(teamId, requesterId, "withdraw the team");

        if (team.getStatus() == TeamStatus.WITHDRAWN || team.getStatus() == TeamStatus.DISQUALIFIED) {
            throw new BusinessRuleException("BR-TEAM-08", "Team is already " + team.getStatus());
        }
        // Realistic cut-off: withdrawing mid-competition would corrupt rankings —
        // after the event starts, removal goes through the coordinator (disqualify/no-show).
        if (team.getEvent().getStatus() != EventStatus.OPEN) {
            throw new BusinessRuleException("BR-TEAM-08",
                    "Teams can only withdraw before the event starts (event status: "
                    + team.getEvent().getStatus() + ")");
        }

        team.setStatus(TeamStatus.WITHDRAWN);
        return toResponse(teamRepository.save(team));
    }

    // ─── Revoke a PENDING invitation (→ CANCELLED) ───────────────────────────

    @Override
    @Transactional
    public InvitationResponse revokeInvitation(Long teamId, Long invitationId, Long requesterId) {
        findTeam(teamId);
        requireActiveLeader(teamId, requesterId, "revoke invitations");

        TeamInvitation inv = teamInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found: " + invitationId));
        if (!inv.getTeam().getId().equals(teamId)) {
            throw new ResourceNotFoundException(
                    "Invitation " + invitationId + " does not belong to team " + teamId);
        }
        if (inv.getStatus() != InvitationStatus.PENDING) {
            throw new BusinessRuleException("INV-STATUS",
                    "Only PENDING invitations can be revoked (current: " + inv.getStatus() + ")");
        }

        inv.setStatus(InvitationStatus.CANCELLED);
        return InvitationResponse.from(teamInvitationRepository.save(inv));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /** BR-TEAM-04: event must be OPEN and 'now' within [registrationStart, registrationEnd]. */
    private void validateRegistrationOpen(Event event) {
        if (event.getStatus() != EventStatus.OPEN) {
            throw new BusinessRuleException("BR-TEAM-04",
                    "Event is not open for registration (status: " + event.getStatus() + ")");
        }
        LocalDateTime now = LocalDateTime.now();
        if (event.getRegistrationStart() != null && now.isBefore(event.getRegistrationStart())) {
            throw new BusinessRuleException("BR-TEAM-04", "Registration window has not opened yet");
        }
        if (event.getRegistrationEnd() != null && now.isAfter(event.getRegistrationEnd())) {
            throw new BusinessRuleException("BR-TEAM-04", "Registration window has closed");
        }
    }

    /** Requester must be the ACTIVE LEADER of the team. */
    private void requireActiveLeader(Long teamId, Long userId, String action) {
        boolean isLeader = teamMemberRepository.findByTeamId(teamId).stream()
                .anyMatch(m -> m.getUser().getId().equals(userId)
                        && m.getMemberRole() == TeamMemberRole.LEADER
                        && m.getStatus() == TeamMemberStatus.ACTIVE);
        if (!isLeader) {
            throw new ForbiddenActionException("Only the team leader can " + action);
        }
    }

    private TeamResponse toResponse(Team team) {
        List<TeamMemberResponse> members = teamMemberRepository.findByTeamId(team.getId())
                .stream().map(TeamMemberResponse::from).toList();
        return TeamResponse.from(team, members);
    }

    private Team findTeam(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + id));
    }

    private Event findEvent(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + id));
    }

    private Category findCategory(Long categoryId, Long eventId) {
        Category cat = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
        if (!cat.getEvent().getId().equals(eventId)) {
            throw new ResourceNotFoundException(
                    "Category " + categoryId + " does not belong to event " + eventId);
        }
        return cat;
    }
}
