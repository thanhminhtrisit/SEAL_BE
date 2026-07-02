package com.seal.seal_backend.submission.service.impl;

import com.seal.seal_backend.common.exception.BusinessRuleException;
import com.seal.seal_backend.common.exception.ForbiddenActionException;
import com.seal.seal_backend.common.exception.ResourceNotFoundException;
import com.seal.seal_backend.domain.entity.*;
import com.seal.seal_backend.domain.enums.RoundStatus;
import com.seal.seal_backend.domain.enums.SubmissionStatus;
import com.seal.seal_backend.domain.enums.TeamMemberRole;
import com.seal.seal_backend.domain.enums.TeamMemberStatus;
import com.seal.seal_backend.domain.enums.TeamStatus;
import com.seal.seal_backend.domain.repository.*;
import com.seal.seal_backend.submission.dto.request.CreateSubmissionRequestDTO;
import com.seal.seal_backend.submission.dto.response.SubmissionDetailResponseDTO;
import com.seal.seal_backend.submission.dto.response.SubmissionMyOverviewResponseDTO;
import com.seal.seal_backend.submission.dto.response.SubmissionMyRoundOverviewDTO;
import com.seal.seal_backend.submission.dto.response.SubmissionMyTeamOverviewDTO;
import com.seal.seal_backend.submission.dto.response.SubmissionRequirementsDTO;
import com.seal.seal_backend.submission.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final RoundRepository roundRepository;
    private final UserRepository userRepository;
    private final RankingRepository rankingRepository;

    @Override
    @Transactional
    public SubmissionDetailResponseDTO createSubmission(CreateSubmissionRequestDTO request, Long actorUserId) {
        Team team = teamRepository.findByIdForUpdate(request.getTeamId())
                .orElseThrow(() -> new ResourceNotFoundException("Team", request.getTeamId()));
        Round round = findRound(request.getRoundId());
        User actor = findUser(actorUserId);

        validateTeamAndRound(team, round);
        validateTeamCanSubmit(team);
        validateRoundOpenForSubmission(round);
        validateTeamLeader(team.getId(), actor.getId());
        validatePromotedIfRankingExists(team, round);
        validateSubmissionArtifacts(round, request);

        Submission submission = new Submission();
        submission.setTeam(team);
        submission.setRound(round);
        submission.setSubmittedBy(actor);
        submission.setAttemptNumber(submissionRepository.findMaxAttemptNumber(team.getId(), round.getId()) + 1);
        submission.setRepoUrl(trimToNull(request.getRepoUrl()));
        submission.setDemoUrl(trimToNull(request.getDemoUrl()));
        submission.setSlideUrl(trimToNull(request.getSlideUrl()));
        submission.setReportUrl(trimToNull(request.getReportUrl()));
        submission.setChangeNote(trimToNull(request.getChangeNote()));
        submission.setStatus(SubmissionStatus.SUBMITTED);
        submission.setSubmittedAt(LocalDateTime.now());
        return mapSubmission(submissionRepository.save(submission));
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionDetailResponseDTO getCurrentSubmission(
            Long teamId, Long roundId, Long actorUserId, boolean staffViewer) {
        Team team = findTeam(teamId);
        Round round = findRound(roundId);
        validateTeamAndRound(team, round);
        validateCanViewTeam(teamId, actorUserId, staffViewer);

        Submission submission = submissionRepository
                .findFirstByTeamIdAndRoundIdAndStatusOrderByAttemptNumberDesc(
                        teamId, roundId, SubmissionStatus.SUBMITTED)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Submission not found for team " + teamId + " and round " + roundId));

        return mapSubmission(submission);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionDetailResponseDTO> getHistory(
            Long teamId, Long roundId, Long actorUserId, boolean staffViewer) {
        Team team = findTeam(teamId);
        Round round = findRound(roundId);
        validateTeamAndRound(team, round);
        validateCanViewTeam(teamId, actorUserId, staffViewer);

        return submissionRepository.findByTeamIdAndRoundIdOrderByAttemptNumberDesc(teamId, roundId)
                .stream()
                .map(this::mapSubmission)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionMyOverviewResponseDTO getMyOverview(Long actorUserId) {
        findUser(actorUserId);
        List<TeamMember> memberships = teamMemberRepository
                .findByUser_IdAndStatusOrderByJoinedAtDesc(actorUserId, TeamMemberStatus.ACTIVE);

        Map<Long, List<Round>> roundsByEventId = new HashMap<>();
        Map<Long, List<Submission>> submissionsByTeamId = new HashMap<>();
        Map<Long, SubmissionMyTeamOverviewDTO> teamOverviewById = new LinkedHashMap<>();

        for (TeamMember membership : memberships) {
            Team team = membership.getTeam();
            Long eventId = team.getEvent().getId();
            roundsByEventId.computeIfAbsent(
                    eventId, id -> roundRepository.findByEventIdOrderByOrderNumberAsc(id));
            submissionsByTeamId.computeIfAbsent(team.getId(), submissionRepository::findByTeamId);

            SubmissionMyTeamOverviewDTO teamOverview = teamOverviewById.computeIfAbsent(team.getId(), id -> {
                SubmissionMyTeamOverviewDTO dto = new SubmissionMyTeamOverviewDTO();
                dto.setTeamId(team.getId());
                dto.setTeamName(team.getName());
                dto.setEventId(eventId);
                dto.setEventName(team.getEvent().getName());
                dto.setCategoryId(team.getCategory().getId());
                dto.setCategoryName(team.getCategory().getName());
                dto.setMemberRole(membership.getMemberRole().name());
                dto.setRounds(new ArrayList<>());
                return dto;
            });

            if (teamOverview.getRounds().isEmpty()) {
                Map<Long, Submission> currentByRound = new HashMap<>();
                for (Submission candidate : submissionsByTeamId.getOrDefault(team.getId(), List.of())) {
                    Long candidateRoundId = candidate.getRound().getId();
                    Submission current = currentByRound.get(candidateRoundId);
                    if (candidate.getStatus() == SubmissionStatus.SUBMITTED
                            && (current == null
                            || candidate.getAttemptNumber() > current.getAttemptNumber())) {
                        currentByRound.put(candidateRoundId, candidate);
                    }
                }

                for (Round eventRound : roundsByEventId.getOrDefault(eventId, List.of())) {
                    SubmissionMyRoundOverviewDTO roundOverview = new SubmissionMyRoundOverviewDTO();
                    roundOverview.setRoundId(eventRound.getId());
                    roundOverview.setRoundName(eventRound.getName());
                    roundOverview.setOrderNumber(eventRound.getOrderNumber());
                    roundOverview.setStatus(eventRound.getStatus().name());
                    roundOverview.setSubmissionDeadline(eventRound.getSubmissionDeadline());
                    roundOverview.setSubmissionRequirements(SubmissionRequirementsDTO.from(eventRound));

                    Submission current = currentByRound.get(eventRound.getId());
                    if (current != null) {
                        roundOverview.setSubmission(mapSubmission(current));
                    }
                    teamOverview.getRounds().add(roundOverview);
                }
            }
        }

        SubmissionMyOverviewResponseDTO response = new SubmissionMyOverviewResponseDTO();
        response.setTeams(new ArrayList<>(teamOverviewById.values()));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionDetailResponseDTO getSubmission(
            Long submissionId, Long actorUserId, boolean staffViewer) {
        Submission submission = findSubmission(submissionId);
        validateCanViewTeam(submission.getTeam().getId(), actorUserId, staffViewer);
        return mapSubmission(submission);
    }

    private Team findTeam(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", teamId));
    }

    private Round findRound(Long roundId) {
        return roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round", roundId));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private Submission findSubmission(Long submissionId) {
        return submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission", submissionId));
    }

    private void validateTeamAndRound(Team team, Round round) {
        if (!team.getEvent().getId().equals(round.getEvent().getId())) {
            throw new BusinessRuleException("BR-SUB-03", "Team and round must belong to the same event");
        }
    }

    private void validateTeamCanSubmit(Team team) {
        if (team.getStatus() != TeamStatus.ACTIVE) {
            throw new BusinessRuleException("BR-SUB-04", "Only active teams can submit projects");
        }
    }

    private void validateRoundOpenForSubmission(Round round) {
        if (round.getStatus() != RoundStatus.OPEN_FOR_SUBMISSION) {
            throw new BusinessRuleException("BR-SUB-01", "Round is not open for submission");
        }
        if (round.getSubmissionDeadline() != null
                && LocalDateTime.now().isAfter(round.getSubmissionDeadline())) {
            throw new BusinessRuleException("BR-SUB-01", "Submission deadline has passed");
        }
    }

    private void validateTeamLeader(Long teamId, Long userId) {
        boolean activeLeader = teamMemberRepository.existsByTeamIdAndUserIdAndMemberRoleAndStatus(
                teamId, userId, TeamMemberRole.LEADER, TeamMemberStatus.ACTIVE);
        if (!activeLeader) {
            throw new ForbiddenActionException(
                    "Only the active team leader can perform this submission action");
        }
    }

    private void validateCanViewTeam(Long teamId, Long actorUserId, boolean staffViewer) {
        if (staffViewer) {
            return;
        }
        if (!teamMemberRepository.existsByTeamIdAndUserIdAndStatus(
                teamId, actorUserId, TeamMemberStatus.ACTIVE)) {
            throw new ForbiddenActionException(
                    "Only active team members or authorized staff can view this submission");
        }
    }

    private void validatePromotedIfRankingExists(Team team, Round round) {
        if (round.getOrderNumber() == null || round.getOrderNumber() <= 1) {
            return;
        }
        roundRepository.findByEventIdAndOrderNumber(
                round.getEvent().getId(), round.getOrderNumber() - 1).ifPresent(previousRound -> {
            boolean hasRanking =
                    rankingRepository.existsByRoundIdAndTeamId(previousRound.getId(), team.getId());
            boolean promoted = rankingRepository
                    .existsByRoundIdAndTeamIdAndIsPromotedTrue(previousRound.getId(), team.getId());
            if (hasRanking && !promoted) {
                throw new BusinessRuleException(
                        "BR-SUB-05", "Only promoted teams can submit to the next round");
            }
        });
    }

    private void validateSubmissionArtifacts(Round round, CreateSubmissionRequestDTO request) {
        validateArtifact(round.getRequiresRepo(), request.getRepoUrl(), "Repository URL");
        validateArtifact(round.getRequiresDemo(), request.getDemoUrl(), "Demo URL");
        validateArtifact(round.getRequiresSlide(), request.getSlideUrl(), "Slide URL");
        validateArtifact(round.getRequiresReport(), request.getReportUrl(), "Report URL");
    }

    private void validateArtifact(Boolean required, String value, String label) {
        if (Boolean.TRUE.equals(required)) {
            if (!StringUtils.hasText(value)) {
                throw new BusinessRuleException("BR-SUB-02", label + " is required for this round");
            }
            validateHttpUrlIfPresent(value, label);
            return;
        }
        if (StringUtils.hasText(value)) {
            throw new BusinessRuleException("BR-SUB-02", label + " is not accepted for this round");
        }
    }

    private void validateHttpUrlIfPresent(String value, String label) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        try {
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme();
            if ((scheme == null
                    || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")))
                    || !StringUtils.hasText(uri.getHost())) {
                throw new URISyntaxException(value, "HTTP/HTTPS URL with a host is required");
            }
        } catch (URISyntaxException ex) {
            throw new BusinessRuleException(
                    "BR-SUB-02", label + " must be a valid HTTP or HTTPS URL");
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private SubmissionDetailResponseDTO mapSubmission(Submission submission) {
        SubmissionDetailResponseDTO dto = new SubmissionDetailResponseDTO();
        dto.setSubmissionId(submission.getId());
        dto.setTeamId(submission.getTeam().getId());
        dto.setTeamName(submission.getTeam().getName());
        dto.setRoundId(submission.getRound().getId());
        dto.setRoundName(submission.getRound().getName());
        dto.setEventId(submission.getRound().getEvent().getId());
        dto.setEventName(submission.getRound().getEvent().getName());
        dto.setCategoryId(submission.getTeam().getCategory().getId());
        dto.setCategoryName(submission.getTeam().getCategory().getName());
        dto.setAttemptNumber(submission.getAttemptNumber());
        dto.setRepoUrl(submission.getRepoUrl());
        dto.setDemoUrl(submission.getDemoUrl());
        dto.setSlideUrl(submission.getSlideUrl());
        dto.setReportUrl(submission.getReportUrl());
        dto.setChangeNote(submission.getChangeNote());
        dto.setSubmittedBy(submission.getSubmittedBy().getId());
        dto.setStatus(submission.getStatus().name());
        dto.setSubmittedAt(submission.getSubmittedAt());
        dto.setLastUpdatedAt(submission.getUpdatedAt());
        return dto;
    }

}
