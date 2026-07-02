package com.seal.seal_backend.team;

import com.seal.seal_backend.common.audit.AuditPublisher;
import com.seal.seal_backend.common.exception.BusinessRuleException;
import com.seal.seal_backend.common.exception.ForbiddenActionException;
import com.seal.seal_backend.common.exception.ResourceNotFoundException;
import com.seal.seal_backend.domain.entity.*;
import com.seal.seal_backend.domain.enums.*;
import com.seal.seal_backend.domain.repository.*;
import com.seal.seal_backend.team.dto.request.*;
import com.seal.seal_backend.team.dto.response.*;
import com.seal.seal_backend.team.service.impl.TeamServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamServiceImplTest {

    @Mock TeamRepository teamRepository;
    @Mock TeamMemberRepository teamMemberRepository;
    @Mock TeamInvitationRepository teamInvitationRepository;
    @Mock EventRepository eventRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock UserRepository userRepository;
    @Mock AuditPublisher auditPublisher;

    @InjectMocks TeamServiceImpl service;

    private Event sampleEvent;
    private Category sampleCategory;
    private User sampleLeader;
    private Team sampleTeam;
    private TeamMember leaderMember;

    @BeforeEach
    void setup() {
        sampleLeader = new User();
        sampleLeader.setId(7L);
        sampleLeader.setEmail("leader@student.local");
        sampleLeader.setFullName("Team Leader");

        sampleEvent = new Event();
        sampleEvent.setId(1L);
        sampleEvent.setName("SEAL Summer 2026");
        sampleEvent.setStatus(EventStatus.OPEN);
        sampleEvent.setRegistrationStart(LocalDateTime.now().minusDays(30));
        sampleEvent.setRegistrationEnd(LocalDateTime.now().plusDays(30));

        sampleCategory = new Category();
        sampleCategory.setId(1L);
        sampleCategory.setName("Web Application");
        sampleCategory.setEvent(sampleEvent);
        sampleCategory.setIsActive(true);

        sampleTeam = new Team();
        sampleTeam.setId(1L);
        sampleTeam.setEvent(sampleEvent);
        sampleTeam.setCategory(sampleCategory);
        sampleTeam.setLeader(sampleLeader);
        sampleTeam.setName("Code Seals");
        sampleTeam.setStatus(TeamStatus.REGISTERED);

        leaderMember = new TeamMember();
        leaderMember.setTeam(sampleTeam);
        leaderMember.setUser(sampleLeader);
        leaderMember.setMemberRole(TeamMemberRole.LEADER);
        leaderMember.setStatus(TeamMemberStatus.ACTIVE);
    }

    // ─── BR-TEAM-02: 1 user 1 team per event ─────────────────────────────────

    @Nested
    class CreateTeam {

        @Test
        void userAlreadyInTeam_throws_BR_TEAM_02() {
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(sampleCategory));
            when(userRepository.findById(7L)).thenReturn(Optional.of(sampleLeader));
            when(teamRepository.existsActiveMemberByUserIdAndEventId(7L, 1L)).thenReturn(true);

            assertThatThrownBy(() -> service.createTeam(
                    new CreateTeamRequest("New Team", null, 1L, 1L), 7L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-TEAM-02");
        }

        @Test
        void duplicateTeamName_throws_TEAM_NAME_CONFLICT_not_BR_TEAM_02() {
            // FIX #4: duplicate name must NOT use BR-TEAM-02
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(sampleCategory));
            when(userRepository.findById(7L)).thenReturn(Optional.of(sampleLeader));
            when(teamRepository.existsActiveMemberByUserIdAndEventId(7L, 1L)).thenReturn(false);
            when(teamRepository.existsByEventIdAndName(1L, "Code Seals")).thenReturn(true);

            assertThatThrownBy(() -> service.createTeam(
                    new CreateTeamRequest("Code Seals", null, 1L, 1L), 7L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "TEAM-NAME-CONFLICT");
        }

        @Test
        void eventNotFound_throws_ResourceNotFound() {
            when(eventRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createTeam(
                    new CreateTeamRequest("Team X", null, 99L, 1L), 7L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void eventNotOpen_throws_BR_TEAM_04() {
            sampleEvent.setStatus(EventStatus.DRAFT);
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(sampleCategory));

            assertThatThrownBy(() -> service.createTeam(
                    new CreateTeamRequest("Team X", null, 1L, 1L), 7L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-TEAM-04");
        }

        @Test
        void registrationWindowNotStarted_throws_BR_TEAM_04() {
            sampleEvent.setRegistrationStart(LocalDateTime.now().plusDays(5));
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(sampleCategory));

            assertThatThrownBy(() -> service.createTeam(
                    new CreateTeamRequest("Team X", null, 1L, 1L), 7L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-TEAM-04");
        }

        @Test
        void registrationWindowClosed_throws_BR_TEAM_04() {
            sampleEvent.setRegistrationEnd(LocalDateTime.now().minusDays(1));
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(sampleCategory));

            assertThatThrownBy(() -> service.createTeam(
                    new CreateTeamRequest("Team X", null, 1L, 1L), 7L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-TEAM-04");
        }

        @Test
        void validRequest_savesTeamAndLeaderMember() {
            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(sampleCategory));
            when(userRepository.findById(7L)).thenReturn(Optional.of(sampleLeader));
            when(teamRepository.existsActiveMemberByUserIdAndEventId(7L, 1L)).thenReturn(false);
            when(teamRepository.existsByEventIdAndName(1L, "Code Seals")).thenReturn(false);
            when(teamRepository.save(any())).thenReturn(sampleTeam);
            when(teamMemberRepository.findByTeamId(1L)).thenReturn(List.of(leaderMember));

            TeamResponse resp = service.createTeam(
                    new CreateTeamRequest("Code Seals", null, 1L, 1L), 7L);

            assertThat(resp.id()).isEqualTo(1L);
            assertThat(resp.members()).hasSize(1);
            assertThat(resp.members().get(0).role()).isEqualTo(TeamMemberRole.LEADER);
            verify(teamMemberRepository).save(argThat(m -> m.getMemberRole() == TeamMemberRole.LEADER));
        }
    }

    // ─── BR-TEAM-03 CORRECTED: each team has 1 category; multiple teams allowed in same category ─

    @Nested
    class RegisterCategory {

        @Test
        void nonLeader_throws_ForbiddenAction() {
            when(teamRepository.findById(1L)).thenReturn(Optional.of(sampleTeam));
            when(teamMemberRepository.findByTeamId(1L)).thenReturn(List.of(leaderMember));

            // userId 8L is NOT the leader
            assertThatThrownBy(() -> service.registerCategory(1L,
                    new RegisterTeamCategoryRequest(2L), 8L))
                    .isInstanceOf(ForbiddenActionException.class);
        }

        @Test
        void approvedTeam_cannotChangeCategory_throws_BR_TEAM_03() {
            // Status check fires before findCategory → no categoryRepository stub needed
            sampleTeam.setStatus(TeamStatus.APPROVED);

            when(teamRepository.findById(1L)).thenReturn(Optional.of(sampleTeam));
            when(teamMemberRepository.findByTeamId(1L)).thenReturn(List.of(leaderMember));

            assertThatThrownBy(() -> service.registerCategory(1L,
                    new RegisterTeamCategoryRequest(2L), 7L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-TEAM-03");
        }

        @Test
        void multipleTeams_canRegisterSameCategory() {
            // FIX #2: multiple teams in the same category is ALLOWED
            Category cat1 = sampleCategory;

            Team team2 = new Team();
            team2.setId(2L);
            team2.setEvent(sampleEvent);
            team2.setCategory(buildCategory(2L, "Mobile App")); // starts in different cat
            team2.setLeader(sampleLeader);
            team2.setName("Seal Alpha");
            team2.setStatus(TeamStatus.REGISTERED);

            TeamMember leader2 = new TeamMember();
            leader2.setTeam(team2);
            leader2.setUser(sampleLeader);
            leader2.setMemberRole(TeamMemberRole.LEADER);
            leader2.setStatus(TeamMemberStatus.ACTIVE);

            when(teamRepository.findById(2L)).thenReturn(Optional.of(team2));
            when(teamMemberRepository.findByTeamId(2L)).thenReturn(List.of(leader2));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat1));
            when(teamRepository.save(any())).thenReturn(team2);
            when(teamMemberRepository.findByTeamId(2L)).thenReturn(List.of(leader2));

            // Team 2 switches to cat 1 — this must NOT throw (team 1 is already in cat 1)
            assertThatNoException().isThrownBy(() ->
                    service.registerCategory(2L, new RegisterTeamCategoryRequest(1L), 7L));
        }
    }

    // ─── Accept invitation ────────────────────────────────────────────────────

    @Nested
    class AcceptInvitation {

        private TeamInvitation pendingInvitation;
        private User invitee;

        @BeforeEach
        void invSetup() {
            invitee = new User();
            invitee.setId(8L);
            invitee.setEmail("member@student.local");
            invitee.setFullName("New Member");

            pendingInvitation = new TeamInvitation();
            pendingInvitation.setId(10L);
            pendingInvitation.setTeam(sampleTeam);
            pendingInvitation.setEmail("member@student.local");
            pendingInvitation.setStatus(InvitationStatus.PENDING);
            pendingInvitation.setExpiresAt(LocalDateTime.now().plusDays(7));
        }

        @Test
        void validAccept_createsTeamMember_and_setsAccepted() {
            when(teamInvitationRepository.findById(10L)).thenReturn(Optional.of(pendingInvitation));
            when(userRepository.findById(8L)).thenReturn(Optional.of(invitee));
            when(teamRepository.existsActiveMemberByUserIdAndEventId(8L, 1L)).thenReturn(false);
            when(teamMemberRepository.countActiveByTeamId(1L)).thenReturn(1L); // just leader
            when(teamMemberRepository.save(any(TeamMember.class))).thenAnswer(inv -> inv.getArgument(0));
            when(teamInvitationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(teamMemberRepository.findByTeamId(1L)).thenReturn(List.of(leaderMember));

            TeamResponse resp = service.acceptInvitation(10L, 8L);

            assertThat(resp.id()).isEqualTo(1L);
            verify(teamMemberRepository).save(argThat(m ->
                    m.getMemberRole() == TeamMemberRole.MEMBER
                            && m.getStatus() == TeamMemberStatus.ACTIVE));
            verify(teamInvitationRepository).save(argThat(i ->
                    i.getStatus() == InvitationStatus.ACCEPTED));
        }

        @Test
        void acceptWhenAlreadyInEventTeam_throws_BR_TEAM_02() {
            when(teamInvitationRepository.findById(10L)).thenReturn(Optional.of(pendingInvitation));
            when(userRepository.findById(8L)).thenReturn(Optional.of(invitee));
            when(teamRepository.existsActiveMemberByUserIdAndEventId(8L, 1L)).thenReturn(true);

            assertThatThrownBy(() -> service.acceptInvitation(10L, 8L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-TEAM-02");
        }

        @Test
        void acceptWhenTeamFull_throws_BR_TEAM_01() {
            when(teamInvitationRepository.findById(10L)).thenReturn(Optional.of(pendingInvitation));
            when(userRepository.findById(8L)).thenReturn(Optional.of(invitee));
            when(teamRepository.existsActiveMemberByUserIdAndEventId(8L, 1L)).thenReturn(false);
            when(teamMemberRepository.countActiveByTeamId(1L)).thenReturn(5L); // already at max

            assertThatThrownBy(() -> service.acceptInvitation(10L, 8L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-TEAM-01");
        }

        @Test
        void invitationNotFound_throws_ResourceNotFound() {
            when(teamInvitationRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.acceptInvitation(99L, 8L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── BR-TEAM-06: duplicate invitation ────────────────────────────────────

    @Nested
    class InviteMember {

        @Test
        void duplicateEmail_throws_BR_TEAM_06() {
            when(teamRepository.findById(1L)).thenReturn(Optional.of(sampleTeam));
            when(userRepository.findById(7L)).thenReturn(Optional.of(sampleLeader));
            when(teamMemberRepository.findByTeamId(1L)).thenReturn(List.of(leaderMember));
            when(teamInvitationRepository.existsByTeamIdAndEmail(1L, "member@student.local"))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.inviteMember(1L,
                    new InviteMemberRequest("member@student.local"), 7L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-TEAM-06");
        }

        @Test
        void inviteeAlreadyInEventTeam_throws_BR_TEAM_06() {
            User invitee = new User();
            invitee.setId(9L);
            invitee.setEmail("taken@student.local");

            when(teamRepository.findById(1L)).thenReturn(Optional.of(sampleTeam));
            when(userRepository.findById(7L)).thenReturn(Optional.of(sampleLeader));
            when(teamMemberRepository.findByTeamId(1L)).thenReturn(List.of(leaderMember));
            when(teamInvitationRepository.existsByTeamIdAndEmail(1L, "taken@student.local"))
                    .thenReturn(false);
            when(userRepository.findByEmail("taken@student.local")).thenReturn(Optional.of(invitee));
            when(teamRepository.existsActiveMemberByUserIdAndEventId(9L, 1L)).thenReturn(true);

            assertThatThrownBy(() -> service.inviteMember(1L,
                    new InviteMemberRequest("taken@student.local"), 7L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-TEAM-06");
        }

        @Test
        void teamNotFound_throws_ResourceNotFound() {
            when(teamRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.inviteMember(99L,
                    new InviteMemberRequest("x@y.com"), 7L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── FR-TEAM-05: Approve / reject ─────────────────────────────────────────

    @Nested
    class ReviewTeam {

        @Test
        void approveWithTooFewMembers_throws_BR_TEAM_01() {
            when(teamRepository.findById(1L)).thenReturn(Optional.of(sampleTeam));
            when(userRepository.findById(3L)).thenReturn(Optional.of(sampleLeader));
            when(teamMemberRepository.countActiveByTeamId(1L)).thenReturn(2L); // < 3

            assertThatThrownBy(() -> service.reviewTeam(1L,
                    new ApproveTeamRequest(true, null), 3L, "127.0.0.1"))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-TEAM-01");
        }

        @Test
        void approveWithSufficientMembers_succeeds() {
            // After 3 members accept invitations, approval should go through
            User coordinator = new User();
            coordinator.setId(3L);
            coordinator.setEmail("coord@seal.local");

            Team approvedTeam = new Team();
            approvedTeam.setId(1L);
            approvedTeam.setEvent(sampleEvent);
            approvedTeam.setCategory(sampleCategory);
            approvedTeam.setLeader(sampleLeader);
            approvedTeam.setName("Code Seals");
            approvedTeam.setStatus(TeamStatus.APPROVED);

            when(teamRepository.findById(1L)).thenReturn(Optional.of(sampleTeam));
            when(userRepository.findById(3L)).thenReturn(Optional.of(coordinator));
            when(teamMemberRepository.countActiveByTeamId(1L)).thenReturn(3L); // exactly min
            when(teamRepository.save(any())).thenReturn(approvedTeam);
            when(teamMemberRepository.findByTeamId(1L)).thenReturn(List.of(leaderMember));

            TeamResponse resp = service.reviewTeam(1L,
                    new ApproveTeamRequest(true, null), 3L, "127.0.0.1");

            assertThat(resp.status()).isEqualTo(TeamStatus.APPROVED);
            verify(auditPublisher).log(any(), eq(com.seal.seal_backend.common.audit.AuditAction.TEAM_APPROVED),
                    any(), any(), any(), any(), any(), any());
        }
    }

    // ─── FIX 1: listInvitations authz ─────────────────────────────────────────

    @Nested
    class ListInvitations {

        @Test
        void coordinator_canViewInvitations() {
            when(teamRepository.findById(1L)).thenReturn(Optional.of(sampleTeam));
            when(teamInvitationRepository.findByTeamIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

            assertThatNoException().isThrownBy(() -> service.listInvitations(1L, 3L, "COORDINATOR"));
        }

        @Test
        void superCoordinator_canViewInvitations() {
            when(teamRepository.findById(1L)).thenReturn(Optional.of(sampleTeam));
            when(teamInvitationRepository.findByTeamIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

            assertThatNoException().isThrownBy(() -> service.listInvitations(1L, 99L, "SUPER_COORDINATOR"));
        }

        @Test
        void activeMemberOfTeam_canView() {
            User member = new User();
            member.setId(8L);
            member.setEmail("member@student.local");
            TeamMember tm = new TeamMember();
            tm.setTeam(sampleTeam);
            tm.setUser(member);
            tm.setMemberRole(TeamMemberRole.MEMBER);
            tm.setStatus(TeamMemberStatus.ACTIVE);

            when(teamRepository.findById(1L)).thenReturn(Optional.of(sampleTeam));
            when(teamMemberRepository.findByTeamId(1L)).thenReturn(List.of(leaderMember, tm));
            when(teamInvitationRepository.findByTeamIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

            assertThatNoException().isThrownBy(() -> service.listInvitations(1L, 8L, "TEAM_MEMBER"));
        }

        @Test
        void userOutsideTeam_notCoordinator_throws_ForbiddenAction() {
            when(teamRepository.findById(1L)).thenReturn(Optional.of(sampleTeam));
            // only the leader is in the team; outsider id=99 is not
            when(teamMemberRepository.findByTeamId(1L)).thenReturn(List.of(leaderMember));

            assertThatThrownBy(() -> service.listInvitations(1L, 99L, "TEAM_MEMBER"))
                    .isInstanceOf(ForbiddenActionException.class);
        }

        @Test
        void adminRole_cannotView_throws_ForbiddenAction() {
            when(teamRepository.findById(1L)).thenReturn(Optional.of(sampleTeam));
            when(teamMemberRepository.findByTeamId(1L)).thenReturn(List.of(leaderMember));

            assertThatThrownBy(() -> service.listInvitations(1L, 1L, "ADMIN"))
                    .isInstanceOf(ForbiddenActionException.class);
        }
    }

    // ─── FIX 2: listMyInvitations ─────────────────────────────────────────────

    @Nested
    class ListMyInvitations {

        @Test
        void returnsPendingInvitationsForEmail() {
            TeamInvitation pending = new TeamInvitation();
            pending.setId(10L);
            pending.setTeam(sampleTeam);
            pending.setEmail("invited@test.local");
            pending.setStatus(InvitationStatus.PENDING);
            pending.setInvitedBy(sampleLeader);
            pending.setExpiresAt(java.time.LocalDateTime.now().plusDays(7));

            when(teamInvitationRepository.findByEmailAndStatus("invited@test.local", InvitationStatus.PENDING))
                    .thenReturn(List.of(pending));

            var result = service.listMyInvitations("invited@test.local");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).invitationId()).isEqualTo(10L);
            assertThat(result.get(0).teamId()).isEqualTo(1L);
            assertThat(result.get(0).teamName()).isEqualTo("Code Seals");
            assertThat(result.get(0).eventId()).isEqualTo(1L);
            assertThat(result.get(0).invitedByName()).isEqualTo("Team Leader");
        }

        @Test
        void noInvitations_returnsEmptyList() {
            when(teamInvitationRepository.findByEmailAndStatus("nobody@test.local", InvitationStatus.PENDING))
                    .thenReturn(List.of());

            assertThat(service.listMyInvitations("nobody@test.local")).isEmpty();
        }
    }

    // ─── listTeamsByEvent with status filter ──────────────────────────────────

    @Nested
    class ListTeamsByEvent {

        @Test
        void noFilter_returnsAllTeams() {
            Team t2 = new Team();
            t2.setId(2L);
            t2.setEvent(sampleEvent);
            t2.setCategory(sampleCategory);
            t2.setLeader(sampleLeader);
            t2.setName("Seal Beta");
            t2.setStatus(TeamStatus.APPROVED);

            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
            when(teamRepository.findByEventIdOrderByCreatedAtAsc(1L))
                    .thenReturn(List.of(sampleTeam, t2));

            var result = service.listTeamsByEvent(1L, null);

            assertThat(result).hasSize(2);
        }

        @Test
        void filterApproved_returnsOnlyApprovedTeams() {
            Team approved = new Team();
            approved.setId(2L);
            approved.setEvent(sampleEvent);
            approved.setCategory(sampleCategory);
            approved.setLeader(sampleLeader);
            approved.setName("Seal Approved");
            approved.setStatus(TeamStatus.APPROVED);

            when(eventRepository.findById(1L)).thenReturn(Optional.of(sampleEvent));
            when(teamRepository.findByEventIdAndStatusOrderByCreatedAtAsc(1L, TeamStatus.APPROVED))
                    .thenReturn(List.of(approved));

            var result = service.listTeamsByEvent(1L, TeamStatus.APPROVED);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).status()).isEqualTo(TeamStatus.APPROVED);
        }

        @Test
        void eventNotFound_throws_ResourceNotFound() {
            when(eventRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.listTeamsByEvent(99L, null))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Category buildCategory(Long id, String name) {
        Category cat = new Category();
        cat.setId(id);
        cat.setName(name);
        cat.setEvent(sampleEvent);
        cat.setIsActive(true);
        return cat;
    }
}
