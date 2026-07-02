package com.seal.seal_backend.submission.service;

import com.seal.seal_backend.submission.dto.request.CreateSubmissionRequestDTO;
import com.seal.seal_backend.submission.dto.response.SubmissionDetailResponseDTO;
import com.seal.seal_backend.submission.dto.response.SubmissionMyOverviewResponseDTO;

import java.util.List;

public interface SubmissionService {
    SubmissionDetailResponseDTO createSubmission(CreateSubmissionRequestDTO request, Long actorUserId);

    SubmissionDetailResponseDTO getCurrentSubmission(Long teamId, Long roundId, Long actorUserId, boolean staffViewer);

    SubmissionMyOverviewResponseDTO getMyOverview(Long actorUserId);

    SubmissionDetailResponseDTO getSubmission(Long submissionId, Long actorUserId, boolean staffViewer);

    List<SubmissionDetailResponseDTO> getHistory(Long teamId, Long roundId, Long actorUserId, boolean staffViewer);

}
