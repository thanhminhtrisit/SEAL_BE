package com.seal.seal_backend.submission.service;

import com.seal.seal_backend.domain.enums.SubmissionStatus;
import com.seal.seal_backend.submission.dto.request.CreateSubmissionRequestDTO;
import com.seal.seal_backend.submission.dto.request.CreateVersionRequestDTO;
import com.seal.seal_backend.submission.dto.request.UpdateSubmissionVersionRequestDTO;
import com.seal.seal_backend.submission.dto.response.SubmissionDetailResponseDTO;
import com.seal.seal_backend.submission.dto.response.SubmissionResponseDTO;
import com.seal.seal_backend.submission.dto.response.SubmissionVersionResponseDTO;

import java.util.List;

public interface SubmissionService {
    SubmissionResponseDTO createSubmission(CreateSubmissionRequestDTO request, Long userId);

    SubmissionDetailResponseDTO getSubmission(Long submissionId);

    SubmissionVersionResponseDTO getCurrentVersion(Long submissionId);

    List<SubmissionVersionResponseDTO> getVersions(Long submissionId);

    SubmissionVersionResponseDTO createVersion(
            Long submissionId,
            CreateVersionRequestDTO request,
            Long userId
    );

    void updateStatus(
            Long submissionId,
            SubmissionStatus status
    );

    List<SubmissionDetailResponseDTO> getByTeam(Long teamId);

    SubmissionDetailResponseDTO getByTeamAndRound(
            Long teamId,
            Long roundId
    );

    List<SubmissionDetailResponseDTO> getByRound(Long roundId);

    SubmissionVersionResponseDTO updateVersion(
            Long versionId,
            UpdateSubmissionVersionRequestDTO request
    );

    void selectCurrentVersion(
            Long submissionId,
            Long versionId
    );

    List<SubmissionDetailResponseDTO> getAllSubmissions();
}
