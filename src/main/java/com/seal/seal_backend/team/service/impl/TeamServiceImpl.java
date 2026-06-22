package com.seal.seal_backend.team.service.impl;

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

    private static final int TEAM_MIN_SIZE = 3;
    private static final int TEAM_MAX_SIZE = 5;

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamInvitationRepository teamInvitationRepository;
    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final AuditPublisher auditPublisher;

    // ─── FR-TEAM-01: Create team ──────────────────────────────────────────────

    @Override
    @Transactional
    public TeamResponse createTeam(CreateTeamRequest req, Long creatorId) {
        Event event = findEvent(req.eventId());
        Category category = findCategory(req.categoryId(), req.eventId());

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
    public List<TeamSummaryResponse> listTeamsByEvent(Long eventId) {
        findEvent(eventId);
        return teamRepository.findByEventIdOrderByCreatedAtAsc(eventId)
                .stream().map(TeamSummaryResponse::from).toList();
    }

    // ─── FR-TEAM-03: Invite member ────────────────────────────────────────────

    @Override
    @Transactional
    public InvitationResponse inviteMember(Long teamId, InviteMemberRequest req, Long inviterId) {
        Team team = findTeam(teamId);
        User inviter = userRepository.findById(inviterId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + inviterId));

        // Only LEADER may invite
        boolean isLeader = teamMemberRepository.findByTeamId(teamId).stream()
                .anyMatch(m -> m.getUser().getId().equals(inviterId)
                        && m.getMemberRole() == TeamMemberRole.LEADER
                        && m.getStatus() == TeamMemberStatus.ACTIVE);
        if (!isLeader) {
            throw new ForbiddenActionException("Only the team leader can send invitations");
        }

        // BR-TEAM-06: duplicate invite (same team + email)
        if (teamInvitationRepository.existsByTeamIdAndEmail(teamId, req.email())) {
            throw new BusinessRuleException("BR-TEAM-06",
                    "An invitation has already been sent to " + req.email() + " for this team");
        }

        // BR-TEAM-06: invitee already active in another team for this event
        userRepository.findByEmail(req.email()).ifPresent(invitee -> {
            if (teamRepository.existsActiveMemberByUserIdAndEventId(invitee.getId(), team.getEvent().getId())) {
                throw new BusinessRuleException("BR-TEAM-06",
                        "User " + req.email() + " already belongs to a team in this event");
            }
        });

        // BR-TEAM-01: team must not already be at max size
        long currentSize = teamMemberRepository.countActiveByTeamId(teamId);
        if (currentSize >= TEAM_MAX_SIZE) {
            throw new BusinessRuleException("BR-TEAM-01",
                    "Team already has maximum size of " + TEAM_MAX_SIZE + " members");
        }

        TeamInvitation inv = new TeamInvitation();
        inv.setTeam(team);
        inv.setEmail(req.email());
        inv.setInvitedBy(inviter);
        inv.setStatus(InvitationStatus.PENDING);
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

        // BR-TEAM-02: user must not already belong to any team in this event
        if (teamRepository.existsActiveMemberByUserIdAndEventId(userId, eventId)) {
            throw new BusinessRuleException("BR-TEAM-02",
                    "You already belong to another team in this event");
        }

        // BR-TEAM-01: team must not be at max capacity
        long currentSize = teamMemberRepository.countActiveByTeamId(team.getId());
        if (currentSize >= TEAM_MAX_SIZE) {
            throw new BusinessRuleException("BR-TEAM-01",
                    "Team is already at maximum size of " + TEAM_MAX_SIZE);
        }

        // Create the new TeamMember
        TeamMember member = new TeamMember();
        member.setTeam(team);
        member.setUser(user);
        member.setMemberRole(TeamMemberRole.MEMBER);
        member.setStatus(TeamMemberStatus.ACTIVE);
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
        boolean isLeader = teamMemberRepository.findByTeamId(teamId).stream()
                .anyMatch(m -> m.getUser().getId().equals(requesterId)
                        && m.getMemberRole() == TeamMemberRole.LEADER
                        && m.getStatus() == TeamMemberStatus.ACTIVE);
        if (!isLeader) {
            throw new ForbiddenActionException("Only the team leader can change the category");
        }

        // BR-TEAM-04: must be within the event's registration window
        LocalDateTime now = LocalDateTime.now();
        if (event.getRegistrationStart() != null && now.isBefore(event.getRegistrationStart())) {
            throw new BusinessRuleException("BR-TEAM-04", "Registration window has not opened yet");
        }
        if (event.getRegistrationEnd() != null && now.isAfter(event.getRegistrationEnd())) {
            throw new BusinessRuleException("BR-TEAM-04", "Registration window has closed");
        }

        // FIX #2: REMOVED wrong "one active team per category" block.
        // BR-TEAM-03 means each TEAM has exactly 1 category (enforced by the DB column category_id).
        // Multiple teams are allowed in the same category.
        // Only block: approved/active team cannot change category without re-review.
        if (team.getStatus() == TeamStatus.APPROVED || team.getStatus() == TeamStatus.ACTIVE) {
            throw new BusinessRuleException("BR-TEAM-03",
                    "Cannot change category of an approved/active team without re-registration");
        }

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
            // BR-TEAM-01: active member count must be within [min, max]
            long activeCount = teamMemberRepository.countActiveByTeamId(teamId);
            if (activeCount < TEAM_MIN_SIZE || activeCount > TEAM_MAX_SIZE) {
                throw new BusinessRuleException("BR-TEAM-01",
                        "Team must have " + TEAM_MIN_SIZE + "–" + TEAM_MAX_SIZE
                                + " active members to be approved; current: " + activeCount);
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
                    "The team leader cannot be removed; transfer leadership first");
        }

        target.setStatus(TeamMemberStatus.REMOVED);
        target.setLeftAt(LocalDateTime.now());
        teamMemberRepository.save(target);

        List<TeamMemberResponse> members = teamMemberRepository.findByTeamId(teamId)
                .stream().map(TeamMemberResponse::from).toList();
        return TeamResponse.from(team, members);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

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
