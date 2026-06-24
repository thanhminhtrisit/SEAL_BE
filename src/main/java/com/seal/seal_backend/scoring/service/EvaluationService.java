package com.seal.seal_backend.scoring.service;

import com.seal.seal_backend.scoring.dto.request.SaveScoresRequest;
import com.seal.seal_backend.scoring.dto.request.StartEvaluationRequest;
import com.seal.seal_backend.scoring.dto.request.SubmitEvaluationRequest;
import com.seal.seal_backend.scoring.dto.response.EvaluationResponse;

public interface EvaluationService {

    EvaluationResponse startEvaluation(StartEvaluationRequest request);

    EvaluationResponse getEvaluationById(Long evaluationId);

    EvaluationResponse saveDraftScores(Long evaluationId, SaveScoresRequest request);

    EvaluationResponse submitEvaluation(Long evaluationId, SubmitEvaluationRequest request);
}
