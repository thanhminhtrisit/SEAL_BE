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
import com.seal.seal_backend.submission.dto.request.CreateDraftSubmissionRequestDTO;
import com.seal.seal_backend.submission.dto.request.CreateSubmissionRequestDTO;
import com.seal.seal_backend.submission.dto.request.SubmitSubmissionRequestDTO;
import com.seal.seal_backend.submission.dto.request.UpdateDraftSubmissionRequestDTO;
import com.seal.seal_backend.submission.dto.request.UpdateSubmissionVersionRequestDTO;
import com.seal.seal_backend.submission.dto.response.SubmissionDetailResponseDTO;
import com.seal.seal_backend.submission.dto.response.SubmissionMyOverviewResponseDTO;
import com.seal.seal_backend.submission.dto.response.SubmissionMyRoundOverviewDTO;
import com.seal.seal_backend.submission.dto.response.SubmissionMyTeamOverviewDTO;
import com.seal.seal_backend.submission.dto.response.SubmissionResponseDTO;
import com.seal.seal_backend.submission.dto.response.SubmissionVersionResponseDTO;
import com.seal.seal_backend.submission.service.SubmissionService;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.seal.seal_backend.submission.dto.request.CreateVersionRequestDTO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final SubmissionVersionRepository submissionVersionRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final RoundRepository roundRepository;
    private final UserRepository userRepository;
    private final RankingRepository rankingRepository;

    @Override
    @Transactional
    public SubmissionDetailResponseDTO createDraft(CreateDraftSubmissionRequestDTO request, Long actorUserId) {
        Team team = findTeam(request.getTeamId());
        Round round = findRound(request.getRoundId());
        User actor = findUser(actorUserId);

        validateTeamAndRound(team, round);
        validateTeamCanSubmit(team);
        validateRoundOpenForSubmission(round);
        validateTeamLeader(team.getId(), actor.getId());
        validatePromotedIfRankingExists(team, round);

        Submission submission = submissionRepository
                .findByTeamIdAndRoundId(team.getId(), round.getId())
                .orElseGet(() -> {
                    Submission draft = new Submission();
                    draft.setTeam(team);
                    draft.setRound(round);
                    draft.setStatus(SubmissionStatus.DRAFT);
                    draft.setLastUpdatedAt(LocalDateTime.now());
                    return submissionRepository.save(draft);
                });

        if (submission.getStatus() != SubmissionStatus.DRAFT) {
            throw new BusinessRuleException("BR-SUB-03", "Submission already exists for this team and round");
        }

        if (hasAnyArtifact(request.getRepoUrl(), request.getDemoUrl(), request.getSlideUrl(), request.getReportUrl())) {
            if (!StringUtils.hasText(request.getRepoUrl())) {
                throw new BusinessRuleException("BR-SUB-02", "Repository URL is required when creating a submission version");
            }
            SubmissionVersion version = createVersionEntity(
                    submission,
                    actor,
                    request.getRepoUrl(),
                    request.getDemoUrl(),
                    request.getSlideUrl(),
                    request.getReportUrl(),
                    request.getChangeNote()
            );
            submission.setCurrentVersionId(version.getId());
        }

        submission.setLastUpdatedAt(LocalDateTime.now());
        return mapSubmission(submissionRepository.save(submission));
    }

    @Override
    @Transactional
    public SubmissionDetailResponseDTO updateDraft(Long submissionId, UpdateDraftSubmissionRequestDTO request, Long actorUserId) {
        Submission submission = findSubmission(submissionId);
        User actor = findUser(actorUserId);

        validateTeamCanSubmit(submission.getTeam());
        validateRoundOpenForSubmission(submission.getRound());
        validateTeamLeader(submission.getTeam().getId(), actor.getId());

        if (submission.getStatus() != SubmissionStatus.DRAFT) {
            throw new BusinessRuleException("BR-SUB-03", "Only draft submissions can be updated with this action");
        }

        if (hasAnyArtifact(request.getRepoUrl(), request.getDemoUrl(), request.getSlideUrl(), request.getReportUrl())) {
            SubmissionVersion current = findCurrentVersionOrNull(submission);
            String repoUrl = coalesceText(request.getRepoUrl(), current != null ? current.getRepoUrl() : null);
            if (!StringUtils.hasText(repoUrl)) {
                throw new BusinessRuleException("BR-SUB-02", "Repository URL is required when creating a submission version");
            }
            SubmissionVersion version = createVersionEntity(
                    submission,
                    actor,
                    repoUrl,
                    coalesceText(request.getDemoUrl(), current != null ? current.getDemoUrl() : null),
                    coalesceText(request.getSlideUrl(), current != null ? current.getSlideUrl() : null),
                    coalesceText(request.getReportUrl(), current != null ? current.getReportUrl() : null),
                    request.getChangeNote()
            );
            submission.setCurrentVersionId(version.getId());
        }

        submission.setLastUpdatedAt(LocalDateTime.now());
        return mapSubmission(submissionRepository.save(submission));
    }

    @Override
    @Transactional
    public SubmissionDetailResponseDTO submit(Long submissionId, SubmitSubmissionRequestDTO request, Long actorUserId) {
        return performSubmit(submissionId, request, actorUserId);
    }

    @Override
    @Transactional
    public SubmissionDetailResponseDTO resubmit(Long submissionId, SubmitSubmissionRequestDTO request, Long actorUserId) {
        return performSubmit(submissionId, request, actorUserId);
    }

    private SubmissionDetailResponseDTO performSubmit(Long submissionId, SubmitSubmissionRequestDTO request, Long actorUserId) {
        Submission submission = findSubmission(submissionId);
        User actor = findUser(actorUserId);

        validateSubmissionEditable(submission);
        validateTeamCanSubmit(submission.getTeam());
        validateRoundOpenForSubmission(submission.getRound());
        validateTeamLeader(submission.getTeam().getId(), actor.getId());
        validatePromotedIfRankingExists(submission.getTeam(), submission.getRound());

        if (request != null && hasAnyArtifact(request.getRepoUrl(), request.getDemoUrl(), request.getSlideUrl(), request.getReportUrl())) {
            SubmissionVersion current = findCurrentVersionOrNull(submission);
            String repoUrl = coalesceText(request.getRepoUrl(), current != null ? current.getRepoUrl() : null);
            if (!StringUtils.hasText(repoUrl)) {
                throw new BusinessRuleException("BR-SUB-02", "Repository URL is required when creating a submission version");
            }
            SubmissionVersion version = createVersionEntity(
                    submission,
                    actor,
                    repoUrl,
                    coalesceText(request.getDemoUrl(), current != null ? current.getDemoUrl() : null),
                    coalesceText(request.getSlideUrl(), current != null ? current.getSlideUrl() : null),
                    coalesceText(request.getReportUrl(), current != null ? current.getReportUrl() : null),
                    request.getChangeNote()
            );
            submission.setCurrentVersionId(version.getId());
        }

        SubmissionVersion currentVersion = getCurrentVersionEntity(submission);
        if (!StringUtils.hasText(currentVersion.getRepoUrl())) {
            throw new BusinessRuleException("BR-SUB-02", "Repository URL is required for final submission");
        }

        LocalDateTime now = LocalDateTime.now();
        submission.setStatus(SubmissionStatus.SUBMITTED);
        submission.setSubmittedAt(now);
        submission.setLastUpdatedAt(now);

        return mapSubmission(submissionRepository.save(submission));
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionDetailResponseDTO getCurrentSubmission(Long teamId, Long roundId, Long actorUserId, boolean staffViewer) {
        Team team = findTeam(teamId);
        Round round = findRound(roundId);
        validateTeamAndRound(team, round);

        Submission submission = submissionRepository.findByTeamIdAndRoundId(teamId, roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found for team " + teamId + " and round " + roundId));

        validateCanView(submission, actorUserId, staffViewer);
        return mapSubmission(submission);
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionMyOverviewResponseDTO getMyOverview(Long actorUserId) {
        findUser(actorUserId);

        List<TeamMember> memberships = teamMemberRepository
                .findByUser_IdAndStatusOrderByJoinedAtDesc(actorUserId, TeamMemberStatus.ACTIVE);

        Map<Long, List<Round>> roundsByEventId = new HashMap<>();
        Map<Long, List<Submission>> submissionsByTeamId = new HashMap<>();
        Map<Long, SubmissionVersion> currentVersionById = new HashMap<>();

        for (TeamMember membership : memberships) {
            Team team = membership.getTeam();
            Long eventId = team.getEvent().getId();

            roundsByEventId.computeIfAbsent(eventId, id -> roundRepository.findByEventIdOrderByOrderNumberAsc(id));
            submissionsByTeamId.computeIfAbsent(team.getId(), id -> submissionRepository.findByTeamId(id));
        }

        Set<Long> currentVersionIds = submissionsByTeamId.values().stream()
                .flatMap(List::stream)
                .map(Submission::getCurrentVersionId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        if (!currentVersionIds.isEmpty()) {
            currentVersionById.putAll(
                    StreamSupport.stream(submissionVersionRepository.findAllById(currentVersionIds).spliterator(), false)
                            .collect(Collectors.toMap(SubmissionVersion::getId, version -> version))
            );
        }

        Map<Long, SubmissionMyTeamOverviewDTO> teamOverviewById = new LinkedHashMap<>();

        for (TeamMember membership : memberships) {
            Team team = membership.getTeam();
            List<Round> rounds = roundsByEventId.getOrDefault(team.getEvent().getId(), List.of());
            Map<Long, Submission> submissionByRoundId = submissionsByTeamId
                    .getOrDefault(team.getId(), List.of())
                    .stream()
                    .collect(Collectors.toMap(submission -> submission.getRound().getId(), submission -> submission, (left, right) -> left));

            SubmissionMyTeamOverviewDTO teamOverview = teamOverviewById.computeIfAbsent(team.getId(), id -> {
                SubmissionMyTeamOverviewDTO dto = new SubmissionMyTeamOverviewDTO();
                dto.setTeamId(team.getId());
                dto.setTeamName(team.getName());
                dto.setEventId(team.getEvent().getId());
                dto.setEventName(team.getEvent().getName());
                dto.setCategoryId(team.getCategory().getId());
                dto.setCategoryName(team.getCategory().getName());
                dto.setMemberRole(membership.getMemberRole().name());
                dto.setRounds(new ArrayList<>());
                return dto;
            });

            if (teamOverview.getRounds().isEmpty()) {
                List<SubmissionMyRoundOverviewDTO> roundOverviews = new ArrayList<>();
                for (Round round : rounds) {
                    SubmissionMyRoundOverviewDTO roundOverview = new SubmissionMyRoundOverviewDTO();
                    roundOverview.setRoundId(round.getId());
                    roundOverview.setRoundName(round.getName());
                    roundOverview.setOrderNumber(round.getOrderNumber());
                    roundOverview.setStatus(round.getStatus().name());
                    roundOverview.setSubmissionDeadline(round.getSubmissionDeadline());

                    Submission submission = submissionByRoundId.get(round.getId());
                    if (submission != null) {
                        roundOverview.setSubmission(mapSubmission(submission, currentVersionById.get(submission.getCurrentVersionId())));
                    }

                    roundOverviews.add(roundOverview);
                }
                teamOverview.setRounds(roundOverviews);
            }
        }

        SubmissionMyOverviewResponseDTO response = new SubmissionMyOverviewResponseDTO();
        response.setTeams(new ArrayList<>(teamOverviewById.values()));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionDetailResponseDTO getSubmission(Long submissionId, Long actorUserId, boolean staffViewer) {
        Submission submission = findSubmission(submissionId);
        validateCanView(submission, actorUserId, staffViewer);
        return mapSubmission(submission);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionVersionResponseDTO> getVersions(Long submissionId, Long actorUserId, boolean staffViewer) {
        Submission submission = findSubmission(submissionId);
        validateCanView(submission, actorUserId, staffViewer);

        return submissionVersionRepository
                .findBySubmissionIdOrderByVersionNumberDesc(submissionId)
                .stream()
                .map(this::mapVersion)
                .toList();
    }

    @Override
    @Transactional
    public SubmissionResponseDTO createSubmission(CreateSubmissionRequestDTO request, Long userId) {

        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new RuntimeException("Team not found"));

        Round round = roundRepository.findById(request.getRoundId())
                .orElseThrow(() -> new RuntimeException("Round not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 1. Create Submission
        Submission submission = new Submission();
        submission.setTeam(team);
        submission.setRound(round);
        submission.setStatus(SubmissionStatus.SUBMITTED);
        submission.setSubmittedAt(java.time.LocalDateTime.now());

        submission = submissionRepository.save(submission);

        // 2. Create SubmissionVersion (version 1)
        SubmissionVersion version = new SubmissionVersion();
        version.setSubmission(submission);
        version.setVersionNumber(1);
        version.setRepoUrl(request.getRepoUrl());
        version.setDemoUrl(request.getDemoUrl());
        version.setSlideUrl(request.getSlideUrl());
        version.setReportUrl(request.getReportUrl());
        version.setSubmittedBy(user);
        version.setChangeNote(request.getChangeNote());

        version = submissionVersionRepository.save(version);

        // 3. update submission current version
        submission.setCurrentVersionId(version.getId());
        submission = submissionRepository.save(submission);

        // 4. response
        SubmissionResponseDTO dto = new SubmissionResponseDTO();
        dto.setSubmissionId(submission.getId());
        dto.setTeamId(team.getId());
        dto.setRoundId(round.getId());
        dto.setCurrentVersionId(version.getId());
        dto.setVersionNumber(version.getVersionNumber());
        dto.setRepoUrl(version.getRepoUrl());
        dto.setStatus(submission.getStatus().name());
        dto.setSubmittedAt(submission.getSubmittedAt());

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionDetailResponseDTO getSubmission(Long submissionId) {

        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        SubmissionDetailResponseDTO dto =
                new SubmissionDetailResponseDTO();

        dto.setSubmissionId(submission.getId());
        dto.setTeamId(submission.getTeam().getId());
        dto.setRoundId(submission.getRound().getId());
        dto.setCurrentVersionId(submission.getCurrentVersionId());
        dto.setStatus(submission.getStatus().name());
        dto.setSubmittedAt(submission.getSubmittedAt());
        dto.setLastUpdatedAt(submission.getLastUpdatedAt());

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionVersionResponseDTO getCurrentVersion(
            Long submissionId
    ) {

        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        Long currentVersionId = submission.getCurrentVersionId();

        SubmissionVersion version =
                submissionVersionRepository.findById(currentVersionId)
                        .orElseThrow(() ->
                                new RuntimeException("Version not found"));

        return mapVersion(version);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionVersionResponseDTO> getVersions(
            Long submissionId
    ) {

        return submissionVersionRepository
                .findBySubmissionIdOrderByVersionNumberDesc(submissionId)
                .stream()
                .map(this::mapVersion)
                .toList();
    }


    @Override
    @Transactional
    public SubmissionVersionResponseDTO createVersion(
            Long submissionId,
            CreateVersionRequestDTO request,
            Long userId
    ) {

        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        validateSubmissionEditable(submission);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Integer nextVersion =
                submissionVersionRepository
                        .findTopBySubmissionIdOrderByVersionNumberDesc(
                                submissionId
                        )
                        .map(v -> v.getVersionNumber() + 1)
                        .orElse(1);

        SubmissionVersion version = new SubmissionVersion();

        version.setSubmission(submission);
        version.setVersionNumber(nextVersion);

        version.setRepoUrl(request.getRepoUrl());
        version.setDemoUrl(request.getDemoUrl());
        version.setSlideUrl(request.getSlideUrl());
        version.setReportUrl(request.getReportUrl());

        version.setSubmittedBy(user);
        version.setChangeNote(request.getChangeNote());

        version = submissionVersionRepository.save(version);

        submission.setCurrentVersionId(version.getId());
        submission.setLastUpdatedAt(java.time.LocalDateTime.now());

        submissionRepository.save(submission);

        return mapVersion(version);
    }

    @Override
    @Transactional
    public void updateStatus(
            Long submissionId,
            SubmissionStatus status
    ) {

        Submission submission =
                submissionRepository.findById(submissionId)
                        .orElseThrow(() ->
                                new RuntimeException("Submission not found"));

        submission.setStatus(status);

        submissionRepository.save(submission);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionDetailResponseDTO> getByTeam(Long teamId) {

        return submissionRepository.findByTeamId(teamId)
                .stream()
                .map(this::mapSubmission)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionDetailResponseDTO> getByRound(Long roundId) {

        return submissionRepository.findByRoundId(roundId)
                .stream()
                .map(this::mapSubmission)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionDetailResponseDTO getByTeamAndRound(
            Long teamId,
            Long roundId
    ) {

        Submission submission =
                submissionRepository
                        .findByTeamIdAndRoundId(teamId, roundId)
                        .orElseThrow(() ->
                                new RuntimeException("Submission not found"));

        return mapSubmission(submission);
    }

    @Override
    @Transactional
    public SubmissionVersionResponseDTO updateVersion(
            Long versionId,
            UpdateSubmissionVersionRequestDTO request
    ) {

        SubmissionVersion version =
                submissionVersionRepository.findById(versionId)
                        .orElseThrow(() ->
                                new RuntimeException("Version not found"));

        if (request.getRepoUrl() != null) {
            version.setRepoUrl(request.getRepoUrl());
        }

        if (request.getDemoUrl() != null) {
            version.setDemoUrl(request.getDemoUrl());
        }

        if (request.getSlideUrl() != null) {
            version.setSlideUrl(request.getSlideUrl());
        }

        if (request.getReportUrl() != null) {
            version.setReportUrl(request.getReportUrl());
        }

        if (request.getChangeNote() != null) {
            version.setChangeNote(request.getChangeNote());
        }

        version = submissionVersionRepository.save(version);

        Submission submission = version.getSubmission();
        submission.setLastUpdatedAt(java.time.LocalDateTime.now());

        submissionRepository.save(submission);

        return mapVersion(version);
    }

    @Override
    @Transactional
    public void selectCurrentVersion(
            Long submissionId,
            Long versionId,
            Long actorUserId
    ) {

        Submission submission =
                submissionRepository.findById(submissionId)
                        .orElseThrow(() ->
                                new RuntimeException("Submission not found"));

        User actor = findUser(actorUserId);

        validateTeamCanSubmit(submission.getTeam());
        validateRoundOpenForSubmission(submission.getRound());
        validateTeamLeader(submission.getTeam().getId(), actor.getId());
        validateSubmissionEditable(submission);

        SubmissionVersion version =
                submissionVersionRepository.findById(versionId)
                        .orElseThrow(() ->
                                new RuntimeException("Version not found"));

        if (!version.getSubmission().getId().equals(submissionId)) {
            throw new RuntimeException(
                    "Version does not belong to this submission"
            );
        }

        submission.setCurrentVersionId(versionId);
        submission.setLastUpdatedAt(
                java.time.LocalDateTime.now()
        );

        submissionRepository.save(submission);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubmissionDetailResponseDTO> getAllSubmissions() {

        return submissionRepository.findAll()
                .stream()
                .map(this::mapSubmission)
                .toList();
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

        if (round.getSubmissionDeadline() != null && LocalDateTime.now().isAfter(round.getSubmissionDeadline())) {
            throw new BusinessRuleException("BR-SUB-01", "Submission deadline has passed");
        }
    }

    private void validateTeamLeader(Long teamId, Long userId) {
        boolean activeLeader = teamMemberRepository.existsByTeamIdAndUserIdAndMemberRoleAndStatus(
                teamId,
                userId,
                TeamMemberRole.LEADER,
                TeamMemberStatus.ACTIVE
        );

        if (!activeLeader) {
            throw new ForbiddenActionException("Only the active team leader can perform this submission action");
        }
    }

    private void validateCanView(Submission submission, Long actorUserId, boolean staffViewer) {
        if (staffViewer) {
            return;
        }

        boolean activeMember = teamMemberRepository.existsByTeamIdAndUserIdAndStatus(
                submission.getTeam().getId(),
                actorUserId,
                TeamMemberStatus.ACTIVE
        );

        if (!activeMember) {
            throw new ForbiddenActionException("Only active team members or authorized staff can view this submission");
        }
    }

    private void validatePromotedIfRankingExists(Team team, Round round) {
        if (round.getOrderNumber() == null || round.getOrderNumber() <= 1) {
            return;
        }

        roundRepository.findByEventIdAndOrderNumber(round.getEvent().getId(), round.getOrderNumber() - 1)
                .ifPresent(previousRound -> {
                    boolean hasRanking = rankingRepository.existsByRoundIdAndTeamId(previousRound.getId(), team.getId());
                    boolean promoted = rankingRepository.existsByRoundIdAndTeamIdAndIsPromotedTrue(previousRound.getId(), team.getId());
                    if (hasRanking && !promoted) {
                        throw new BusinessRuleException("BR-SUB-05", "Only promoted teams can submit to the next round");
                    }
                });
    }

    private SubmissionVersion createVersionEntity(
            Submission submission,
            User submittedBy,
            String repoUrl,
            String demoUrl,
            String slideUrl,
            String reportUrl,
            String changeNote
    ) {
        Integer nextVersion =
                submissionVersionRepository
                        .findTopBySubmissionIdOrderByVersionNumberDesc(submission.getId())
                        .map(v -> v.getVersionNumber() + 1)
                        .orElse(1);

        SubmissionVersion version = new SubmissionVersion();
        version.setSubmission(submission);
        version.setVersionNumber(nextVersion);
        version.setRepoUrl(repoUrl);
        version.setDemoUrl(demoUrl);
        version.setSlideUrl(slideUrl);
        version.setReportUrl(reportUrl);
        version.setSubmittedBy(submittedBy);
        version.setChangeNote(changeNote);

        return submissionVersionRepository.save(version);
    }

    private SubmissionVersion getCurrentVersionEntity(Submission submission) {
        if (submission.getCurrentVersionId() == null) {
            throw new BusinessRuleException("BR-SUB-02", "Submission does not have a current version");
        }

        return submissionVersionRepository.findById(submission.getCurrentVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("Submission version", submission.getCurrentVersionId()));
    }

    private SubmissionVersion findCurrentVersionOrNull(Submission submission) {
        if (submission.getCurrentVersionId() == null) {
            return null;
        }

        return submissionVersionRepository.findById(submission.getCurrentVersionId())
                .orElse(null);
    }

    private String coalesceText(String preferred, String fallback) {
        return StringUtils.hasText(preferred) ? preferred : fallback;
    }

    private boolean hasAnyArtifact(String repoUrl, String demoUrl, String slideUrl, String reportUrl) {
        return StringUtils.hasText(repoUrl)
                || StringUtils.hasText(demoUrl)
                || StringUtils.hasText(slideUrl)
                || StringUtils.hasText(reportUrl);
    }

    private SubmissionVersionResponseDTO mapVersion(
            SubmissionVersion version
    ) {

        SubmissionVersionResponseDTO dto =
                new SubmissionVersionResponseDTO();

        dto.setVersionId(version.getId());
        dto.setVersionNumber(version.getVersionNumber());

        dto.setRepoUrl(version.getRepoUrl());
        dto.setDemoUrl(version.getDemoUrl());
        dto.setSlideUrl(version.getSlideUrl());
        dto.setReportUrl(version.getReportUrl());

        dto.setChangeNote(version.getChangeNote());

        dto.setSubmittedBy(version.getSubmittedBy().getId());

        dto.setSubmittedAt(version.getSubmittedAt());

        return dto;
    }

    private SubmissionDetailResponseDTO mapSubmission(
            Submission submission
    ) {
        SubmissionVersion currentVersion = null;
        if (submission.getCurrentVersionId() != null) {
            currentVersion = submissionVersionRepository.findById(submission.getCurrentVersionId()).orElse(null);
        }
        return mapSubmission(submission, currentVersion);
    }

    private SubmissionDetailResponseDTO mapSubmission(
            Submission submission,
            SubmissionVersion currentVersion
    ) {

        SubmissionDetailResponseDTO dto =
                new SubmissionDetailResponseDTO();

        dto.setSubmissionId(submission.getId());
        dto.setTeamId(submission.getTeam().getId());
        dto.setTeamName(submission.getTeam().getName());
        dto.setRoundId(submission.getRound().getId());
        dto.setRoundName(submission.getRound().getName());
        dto.setEventId(submission.getRound().getEvent().getId());
        dto.setEventName(submission.getRound().getEvent().getName());
        dto.setCategoryId(submission.getTeam().getCategory().getId());
        dto.setCategoryName(submission.getTeam().getCategory().getName());

        dto.setCurrentVersionId(submission.getCurrentVersionId());
        if (currentVersion != null) {
            dto.setCurrentVersion(mapVersion(currentVersion));
        }

        dto.setStatus(submission.getStatus().name());

        dto.setSubmittedAt(submission.getSubmittedAt());
        dto.setLastUpdatedAt(submission.getLastUpdatedAt());

        return dto;
    }

    private void validateSubmissionEditable(
            Submission submission) {

        SubmissionStatus status = submission.getStatus();

        if (status == SubmissionStatus.LOCKED) {
            throw new BusinessRuleException("BR-SUB-03",
                    "Submission has been locked"
            );
        }

        if (status == SubmissionStatus.DISQUALIFIED) {
            throw new BusinessRuleException("BR-SUB-04",
                    "Submission has been disqualified"
            );
        }

        if (status == SubmissionStatus.LATE_REJECTED) {
            throw new BusinessRuleException("BR-SUB-01",
                    "Submission was rejected because of deadline violation"
            );
        }
    }

    private void validateStatusTransition(
            SubmissionStatus current,
            SubmissionStatus target
    ) {

        if (target == null) {
            throw new BusinessRuleException("BR-SUB-03", "Submission status is required");
        }

        switch (current) {

            case DRAFT -> {
                if (target != SubmissionStatus.SUBMITTED
                        && target != SubmissionStatus.DISQUALIFIED
                        && target != SubmissionStatus.LATE_REJECTED) {

                    throw new BusinessRuleException("BR-SUB-03",
                            "Invalid status transition"
                    );
                }
            }

            case SUBMITTED -> {
                if (target != SubmissionStatus.LOCKED
                        && target != SubmissionStatus.DISQUALIFIED) {

                    throw new BusinessRuleException("BR-SUB-03",
                            "Invalid status transition"
                    );
                }
            }

            case LOCKED,
                 DISQUALIFIED,
                 LATE_REJECTED ->

                    throw new BusinessRuleException("BR-SUB-03",
                            "Cannot change final status"
                    );
        }
    }
}
