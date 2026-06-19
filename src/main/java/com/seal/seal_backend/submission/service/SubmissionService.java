package com.seal.seal_backend.submission.service;

import com.seal.seal_backend.submission.dto.request.CreateSubmissionRequestDTO;
import com.seal.seal_backend.submission.dto.response.SubmissionResponseDTO;

public interface SubmissionService {
    SubmissionResponseDTO createSubmission(CreateSubmissionRequestDTO request, Long userId);
}
