package com.seal.seal_backend.submission.service.impl;

import com.seal.seal_backend.domain.entity.*;
import com.seal.seal_backend.domain.enums.SubmissionStatus;
import com.seal.seal_backend.domain.repository.*;
import com.seal.seal_backend.submission.dto.request.CreateSubmissionRequestDTO;
import com.seal.seal_backend.submission.dto.response.SubmissionResponseDTO;
import com.seal.seal_backend.submission.service.SubmissionService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}
