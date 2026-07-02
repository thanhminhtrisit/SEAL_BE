package com.seal.seal_backend.scoring.service;

import com.seal.seal_backend.scoring.dto.request.SaveScoresRequest;
import com.seal.seal_backend.scoring.dto.request.StartEvaluationRequest;
import com.seal.seal_backend.scoring.dto.request.SubmitEvaluationRequest;
import com.seal.seal_backend.scoring.dto.response.EvaluationAuditEntryResponse;
import com.seal.seal_backend.scoring.dto.response.EvaluationResponse;
import com.seal.seal_backend.scoring.dto.response.JudgeAssignedSubmissionResponse;

import java.util.List;

public interface EvaluationService {

    List<JudgeAssignedSubmissionResponse> getAssignedSubmissions(Long currentUserId);

    EvaluationResponse startEvaluation(Long currentUserId, StartEvaluationRequest request);

    EvaluationResponse getEvaluationById(Long currentUserId, Long evaluationId);

    List<EvaluationAuditEntryResponse> getEvaluationAudit(Long currentUserId, Long evaluationId);

    EvaluationResponse saveDraftScores(Long currentUserId, Long evaluationId, SaveScoresRequest request);

    EvaluationResponse submitEvaluation(Long currentUserId, Long evaluationId, SubmitEvaluationRequest request);
}
