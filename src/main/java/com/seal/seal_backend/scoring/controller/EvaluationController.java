package com.seal.seal_backend.scoring.controller;

import com.seal.seal_backend.auth.security.UserPrincipal;
import com.seal.seal_backend.common.security.CurrentUser;
import com.seal.seal_backend.common.api.ApiResponse;
import com.seal.seal_backend.scoring.dto.request.SaveScoresRequest;
import com.seal.seal_backend.scoring.dto.request.StartEvaluationRequest;
import com.seal.seal_backend.scoring.dto.request.SubmitEvaluationRequest;
import com.seal.seal_backend.scoring.dto.response.EvaluationAuditEntryResponse;
import com.seal.seal_backend.scoring.dto.response.EvaluationResponse;
import com.seal.seal_backend.scoring.dto.response.JudgeAssignedSubmissionResponse;
import com.seal.seal_backend.scoring.service.EvaluationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping({"/api/scoring", "/api/evaluations"})
@RestController
@RequiredArgsConstructor
@Tag(name = "Scoring")
public class EvaluationController {

    private final EvaluationService evaluationService;

    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.ok("Evaluation module is alive");
    }

    @PreAuthorize("hasRole('JUDGE')")
    @GetMapping("/assigned-submissions")
    public ResponseEntity<List<JudgeAssignedSubmissionResponse>> getAssignedSubmissions(
            @CurrentUser UserPrincipal currentUser
    ) {
        return ResponseEntity.ok(evaluationService.getAssignedSubmissions(currentUser.getId()));
    }

    @PreAuthorize("hasRole('JUDGE')")
    @PostMapping("/start")
    public ResponseEntity<EvaluationResponse> startEvaluation(
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody StartEvaluationRequest request
    ) {
        EvaluationResponse response = evaluationService.startEvaluation(currentUser.getId(), request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('JUDGE')")
    @GetMapping("/{evaluationId}")
    public ResponseEntity<EvaluationResponse> getEvaluationById(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long evaluationId
    ) {
        EvaluationResponse response = evaluationService.getEvaluationById(currentUser.getId(), evaluationId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('JUDGE')")
    @GetMapping("/{evaluationId}/audit")
    public ResponseEntity<List<EvaluationAuditEntryResponse>> getEvaluationAudit(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long evaluationId
    ) {
        return ResponseEntity.ok(evaluationService.getEvaluationAudit(currentUser.getId(), evaluationId));
    }

    @PreAuthorize("hasRole('JUDGE')")
    @PutMapping("/{evaluationId}/draft")
    public ResponseEntity<EvaluationResponse> saveDraftScores(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long evaluationId,
            @Valid @RequestBody SaveScoresRequest request
    ) {
        EvaluationResponse response = evaluationService.saveDraftScores(currentUser.getId(), evaluationId, request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('JUDGE')")
    @PostMapping("/{evaluationId}/submit")
    public ResponseEntity<EvaluationResponse> submitEvaluation(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long evaluationId,
            @RequestBody SubmitEvaluationRequest request
    ) {
        EvaluationResponse response = evaluationService.submitEvaluation(currentUser.getId(), evaluationId, request);
        return ResponseEntity.ok(response);
    }
}
