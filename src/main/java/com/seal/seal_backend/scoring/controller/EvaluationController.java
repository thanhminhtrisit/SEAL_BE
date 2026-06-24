package com.seal.seal_backend.scoring.controller;

import com.seal.seal_backend.scoring.dto.request.SaveScoresRequest;
import com.seal.seal_backend.scoring.dto.request.StartEvaluationRequest;
import com.seal.seal_backend.scoring.dto.request.SubmitEvaluationRequest;
import com.seal.seal_backend.scoring.dto.response.EvaluationResponse;
import com.seal.seal_backend.scoring.service.EvaluationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/evaluations")
@RestController
@RequiredArgsConstructor
@Tag(name = "Evaluation Tracking")
public class EvaluationController {

    private final EvaluationService evaluationService;

    @PreAuthorize("hasRole('Judge')")
    @PostMapping("/start")
    public ResponseEntity<EvaluationResponse> startEvaluation(
            @Valid @RequestBody StartEvaluationRequest request
    ) {
        EvaluationResponse response = evaluationService.startEvaluation(request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('Judge')")
    @GetMapping("/{evaluationId}")
    public ResponseEntity<EvaluationResponse> getEvaluationById(
            @PathVariable Long evaluationId
    ) {
        EvaluationResponse response = evaluationService.getEvaluationById(evaluationId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('Judge')")
    @PutMapping("/{evaluationId}/draft")
    public ResponseEntity<EvaluationResponse> saveDraftScores(
            @PathVariable Long evaluationId,
            @Valid @RequestBody SaveScoresRequest request
    ) {
        EvaluationResponse response = evaluationService.saveDraftScores(evaluationId, request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('Judge')")
    @PostMapping("/{evaluationId}/submit")
    public ResponseEntity<EvaluationResponse> submitEvaluation(
            @PathVariable Long evaluationId,
            @RequestBody SubmitEvaluationRequest request
    ) {
        EvaluationResponse response = evaluationService.submitEvaluation(evaluationId, request);
        return ResponseEntity.ok(response);
    }
}
