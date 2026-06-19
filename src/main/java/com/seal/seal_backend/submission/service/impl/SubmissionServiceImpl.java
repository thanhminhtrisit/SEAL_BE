package com.seal.seal_backend.submission.service.impl;

import com.seal.seal_backend.domain.entity.*;
import com.seal.seal_backend.domain.enums.SubmissionStatus;
import com.seal.seal_backend.domain.repository.*;
import com.seal.seal_backend.submission.dto.request.CreateSubmissionRequestDTO;
import com.seal.seal_backend.submission.dto.request.UpdateSubmissionVersionRequestDTO;
import com.seal.seal_backend.submission.dto.response.SubmissionDetailResponseDTO;
import com.seal.seal_backend.submission.dto.response.SubmissionResponseDTO;
import com.seal.seal_backend.submission.dto.response.SubmissionVersionResponseDTO;
import com.seal.seal_backend.submission.service.SubmissionService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.seal.seal_backend.submission.dto.request.CreateVersionRequestDTO;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final SubmissionVersionRepository submissionVersionRepository;
    private final TeamRepository teamRepository;
    private final RoundRepository roundRepository;
    private final UserRepository userRepository;

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
            Long versionId
    ) {

        Submission submission =
                submissionRepository.findById(submissionId)
                        .orElseThrow(() ->
                                new RuntimeException("Submission not found"));

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

    private void validateSubmissionEditable(
            Submission submission) {

        SubmissionStatus status = submission.getStatus();

        if (status == SubmissionStatus.LOCKED) {
            throw new RuntimeException(
                    "Submission has been locked"
            );
        }

        if (status == SubmissionStatus.DISQUALIFIED) {
            throw new RuntimeException(
                    "Submission has been disqualified"
            );
        }

        if (status == SubmissionStatus.LATE_REJECTED) {
            throw new RuntimeException(
                    "Submission was rejected because of deadline violation"
            );
        }
    }

    private void validateStatusTransition(
            SubmissionStatus current,
            SubmissionStatus target
    ) {

        switch (current) {

            case DRAFT -> {
                if (target != SubmissionStatus.SUBMITTED
                        && target != SubmissionStatus.DISQUALIFIED
                        && target != SubmissionStatus.LATE_REJECTED) {

                    throw new RuntimeException(
                            "Invalid status transition"
                    );
                }
            }

            case SUBMITTED -> {
                if (target != SubmissionStatus.LOCKED
                        && target != SubmissionStatus.DISQUALIFIED) {

                    throw new RuntimeException(
                            "Invalid status transition"
                    );
                }
            }

            case LOCKED,
                 DISQUALIFIED,
                 LATE_REJECTED ->

                    throw new RuntimeException(
                            "Cannot change final status"
                    );
        }
    }
}
