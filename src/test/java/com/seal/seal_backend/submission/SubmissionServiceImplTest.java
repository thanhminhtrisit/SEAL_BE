package com.seal.seal_backend.submission;

import com.seal.seal_backend.common.exception.BusinessRuleException;
import com.seal.seal_backend.domain.entity.*;
import com.seal.seal_backend.domain.enums.RoundStatus;
import com.seal.seal_backend.domain.enums.SubmissionStatus;
import com.seal.seal_backend.domain.enums.TeamMemberRole;
import com.seal.seal_backend.domain.enums.TeamMemberStatus;
import com.seal.seal_backend.domain.enums.TeamStatus;
import com.seal.seal_backend.domain.repository.*;
import com.seal.seal_backend.submission.dto.request.CreateSubmissionRequestDTO;
import com.seal.seal_backend.submission.service.impl.SubmissionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceImplTest {

    @Mock SubmissionRepository submissionRepository;
    @Mock TeamRepository teamRepository;
    @Mock TeamMemberRepository teamMemberRepository;
    @Mock RoundRepository roundRepository;
    @Mock UserRepository userRepository;
    @Mock RankingRepository rankingRepository;

    @InjectMocks SubmissionServiceImpl service;

    private User leader;
    private Team team;
    private Round round;

    @BeforeEach
    void setUp() {
        leader = new User();
        leader.setId(7L);

        Event event = new Event();
        event.setId(1L);
        event.setName("Event");

        Category category = new Category();
        category.setId(1L);
        category.setName("Web");

        team = new Team();
        team.setId(1L);
        team.setName("Team");
        team.setEvent(event);
        team.setCategory(category);
        team.setLeader(leader);
        team.setStatus(TeamStatus.ACTIVE);

        round = new Round();
        round.setId(2L);
        round.setName("Round");
        round.setEvent(event);
        round.setOrderNumber(1);
        round.setStatus(RoundStatus.OPEN_FOR_SUBMISSION);
        round.setSubmissionDeadline(LocalDateTime.now().plusDays(1));

        when(userRepository.findById(7L)).thenReturn(Optional.of(leader));
        lenient().when(teamRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(team));
        lenient().when(roundRepository.findById(2L)).thenReturn(Optional.of(round));
        lenient().when(teamMemberRepository.existsByTeamIdAndUserIdAndMemberRoleAndStatus(
                1L, 7L, TeamMemberRole.LEADER, TeamMemberStatus.ACTIVE)).thenReturn(true);
        lenient().when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> {
            Submission saved = invocation.getArgument(0);
            if (saved.getId() == null) saved.setId(100L);
            return saved;
        });
    }

    @Test
    void submitCreatesOfficialAttemptWithNextNumber() {
        when(submissionRepository.findMaxAttemptNumber(1L, 2L)).thenReturn(1);

        CreateSubmissionRequestDTO request = request("https://github.com/team/second");
        var response = service.createSubmission(request, 7L);

        assertThat(response.getSubmissionId()).isEqualTo(100L);
        assertThat(response.getAttemptNumber()).isEqualTo(2);
        assertThat(response.getStatus()).isEqualTo("SUBMITTED");
        assertThat(response.getSubmittedAt()).isNotNull();
        verify(submissionRepository).save(argThat(saved ->
                saved.getAttemptNumber() == 2
                        && saved.getStatus() == SubmissionStatus.SUBMITTED
                        && saved.getSubmittedAt() != null));
        verify(submissionRepository, never()).findById(anyLong());
    }

    @Test
    void firstSubmitCreatesAttemptOne() {
        when(submissionRepository.findMaxAttemptNumber(1L, 2L)).thenReturn(0);

        var response = service.createSubmission(request("https://github.com/team/first"), 7L);

        assertThat(response.getAttemptNumber()).isEqualTo(1);
    }

    @Test
    void repoIsRequiredWhenRoundRequiresRepo() {
        round.setRequiresRepo(true);

        assertThatThrownBy(() -> service.createSubmission(request(null), 7L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Repository URL is required");

        verify(submissionRepository, never()).findMaxAttemptNumber(anyLong(), anyLong());
        verify(submissionRepository, never()).save(any());
    }

    @Test
    void repoCanBeOmittedWhenRoundDoesNotRequireRepo() {
        round.setRequiresRepo(false);
        when(submissionRepository.findMaxAttemptNumber(1L, 2L)).thenReturn(0);

        var response = service.createSubmission(request(null), 7L);

        assertThat(response.getAttemptNumber()).isEqualTo(1);
        assertThat(response.getRepoUrl()).isNull();
    }

    @Test
    void repoRejectedWhenRoundDoesNotAcceptIt() {
        round.setRequiresRepo(false);

        assertThatThrownBy(() -> service.createSubmission(request("https://github.com/team/project"), 7L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Repository URL is not accepted");

        verify(submissionRepository, never()).findMaxAttemptNumber(anyLong(), anyLong());
        verify(submissionRepository, never()).save(any());
    }

    @Test
    void slideIsRequiredWhenRoundRequiresSlide() {
        round.setRequiresRepo(false);
        round.setRequiresSlide(true);

        assertThatThrownBy(() -> service.createSubmission(request(null), 7L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Slide URL is required");
        verify(submissionRepository, never()).save(any());
    }

    @Test
    void slideRejectedWhenRoundDoesNotAcceptIt() {
        round.setRequiresRepo(true);
        round.setRequiresSlide(false);

        CreateSubmissionRequestDTO request = request("https://github.com/team/project");
        request.setSlideUrl("https://slides.example.com/deck");

        assertThatThrownBy(() -> service.createSubmission(request, 7L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Slide URL is not accepted");

        verify(submissionRepository, never()).findMaxAttemptNumber(anyLong(), anyLong());
        verify(submissionRepository, never()).save(any());
    }

    @Test
    void demoIsRequiredWhenRoundRequiresDemo() {
        round.setRequiresRepo(false);
        round.setRequiresDemo(true);

        assertThatThrownBy(() -> service.createSubmission(request(null), 7L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Demo URL is required");

        verify(submissionRepository, never()).findMaxAttemptNumber(anyLong(), anyLong());
        verify(submissionRepository, never()).save(any());
    }

    @Test
    void demoRejectedWhenRoundDoesNotAcceptIt() {
        round.setRequiresRepo(true);
        round.setRequiresDemo(false);

        CreateSubmissionRequestDTO request = request("https://github.com/team/project");
        request.setDemoUrl("https://demo.example.com");

        assertThatThrownBy(() -> service.createSubmission(request, 7L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Demo URL is not accepted");

        verify(submissionRepository, never()).findMaxAttemptNumber(anyLong(), anyLong());
        verify(submissionRepository, never()).save(any());
    }

    @Test
    void reportRejectedWhenRoundDoesNotAcceptIt() {
        round.setRequiresRepo(true);
        round.setRequiresReport(false);

        CreateSubmissionRequestDTO request = request("https://github.com/team/project");
        request.setReportUrl("https://docs.example.com/report");

        assertThatThrownBy(() -> service.createSubmission(request, 7L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Report URL is not accepted");

        verify(submissionRepository, never()).findMaxAttemptNumber(anyLong(), anyLong());
        verify(submissionRepository, never()).save(any());
    }

    @Test
    void allFinalRoundRequirementsAreEnforced() {
        round.setRequiresRepo(true);
        round.setRequiresDemo(true);
        round.setRequiresSlide(true);
        round.setRequiresReport(true);
        CreateSubmissionRequestDTO request = request("https://github.com/team/final");
        request.setDemoUrl("https://demo.example.com");
        request.setSlideUrl("https://slides.example.com");

        assertThatThrownBy(() -> service.createSubmission(request, 7L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Report URL is required");
        verify(submissionRepository, never()).save(any());
    }

    @Test
    void allRequiredArtifactsCanSubmitSuccessfully() {
        round.setRequiresRepo(true);
        round.setRequiresDemo(true);
        round.setRequiresSlide(true);
        round.setRequiresReport(true);
        when(submissionRepository.findMaxAttemptNumber(1L, 2L)).thenReturn(0);
        CreateSubmissionRequestDTO request = request("https://github.com/team/project");
        request.setDemoUrl("https://demo.example.com");
        request.setSlideUrl("https://slides.example.com/deck");
        request.setReportUrl("https://docs.example.com/report");

        var response = service.createSubmission(request, 7L);

        assertThat(response.getAttemptNumber()).isEqualTo(1);
        assertThat(response.getRepoUrl()).isEqualTo("https://github.com/team/project");
        assertThat(response.getDemoUrl()).isEqualTo("https://demo.example.com");
        assertThat(response.getSlideUrl()).isEqualTo("https://slides.example.com/deck");
        assertThat(response.getReportUrl()).isEqualTo("https://docs.example.com/report");
    }

    @Test
    void invalidRequiredUrlIsRejectedWithoutPersistingAttempt() {
        round.setRequiresRepo(false);
        round.setRequiresDemo(true);
        CreateSubmissionRequestDTO request = request(null);
        request.setDemoUrl("javascript:alert(1)");

        assertThatThrownBy(() -> service.createSubmission(request, 7L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Demo URL must be a valid HTTP or HTTPS URL");

        verify(submissionRepository, never()).findMaxAttemptNumber(anyLong(), anyLong());
        verify(submissionRepository, never()).save(any());
    }

    @Test
    void invalidRepoUrlIsRejectedWithoutPersistingAttempt() {
        round.setRequiresRepo(true);

        assertThatThrownBy(() -> service.createSubmission(request("ftp://github.com/team/project"), 7L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Repository URL must be a valid HTTP or HTTPS URL");

        verify(submissionRepository, never()).findMaxAttemptNumber(anyLong(), anyLong());
        verify(submissionRepository, never()).save(any());
    }

    @Test
    void failedValidationDoesNotAllocateAttemptNumberOrPersistSubmission() {
        round.setRequiresRepo(true);
        round.setRequiresSlide(false);
        CreateSubmissionRequestDTO request = request("https://github.com/team/project");
        request.setSlideUrl("https://slides.example.com/should-reject");

        assertThatThrownBy(() -> service.createSubmission(request, 7L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Slide URL is not accepted");

        verify(submissionRepository, never()).findMaxAttemptNumber(anyLong(), anyLong());
        verify(submissionRepository, never()).save(any());
    }

    @Test
    void myOverviewReturnsNestedSubmissionRequirements() {
        round.setRequiresRepo(false);
        round.setRequiresDemo(true);
        round.setRequiresSlide(true);
        round.setRequiresReport(false);

        TeamMember membership = new TeamMember();
        membership.setTeam(team);
        membership.setUser(leader);
        membership.setMemberRole(TeamMemberRole.LEADER);
        membership.setStatus(TeamMemberStatus.ACTIVE);

        when(teamMemberRepository.findByUser_IdAndStatusOrderByJoinedAtDesc(
                7L, TeamMemberStatus.ACTIVE)).thenReturn(List.of(membership));
        when(roundRepository.findByEventIdOrderByOrderNumberAsc(1L)).thenReturn(List.of(round));
        when(submissionRepository.findByTeamId(1L)).thenReturn(List.of());

        var response = service.getMyOverview(7L);
        var requirements = response.getTeams().getFirst().getRounds().getFirst()
                .getSubmissionRequirements();

        assertThat(requirements.requiresRepo()).isFalse();
        assertThat(requirements.requiresDemo()).isTrue();
        assertThat(requirements.requiresSlide()).isTrue();
        assertThat(requirements.requiresReport()).isFalse();
    }

    @Test
    void submitAfterDeadlineIsRejected() {
        round.setSubmissionDeadline(LocalDateTime.now().minusMinutes(1));

        assertThatThrownBy(() -> service.createSubmission(
                request("https://github.com/team/late"), 7L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("deadline");
        verify(submissionRepository, never()).save(any());
    }

    private CreateSubmissionRequestDTO request(String repoUrl) {
        CreateSubmissionRequestDTO request = new CreateSubmissionRequestDTO();
        request.setTeamId(1L);
        request.setRoundId(2L);
        request.setRepoUrl(repoUrl);
        return request;
    }
}
