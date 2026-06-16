package com.seal.seal_backend.submission.controller;

import com.seal.seal_backend.common.api.ApiResponse;
import com.seal.seal_backend.submission.dto.request.CreateSubmissionRequestDTO;
import com.seal.seal_backend.submission.dto.response.SubmissionResponseDTO;
import com.seal.seal_backend.submission.service.SubmissionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Flow: Project submission tracking
 * OWNER: M2 — Lê Quang Hải
 * Only the owner edits files under the 'submission' package. Put authorization at method level, e.g.
 *   @org.springframework.security.access.prepost.PreAuthorize("hasRole('COORDINATOR')")
 */
@RestController
@RequestMapping("/api/submissions")
@Tag(name = "Submission Tracking")
@RequiredArgsConstructor
public class SubmissionController {

    @Autowired
    private final SubmissionService submissionService;

    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.ok("Submission Tracking module is alive");
    }

    @PostMapping
    public ResponseEntity<SubmissionResponseDTO> createSubmission(
            @RequestBody CreateSubmissionRequestDTO request,
            @RequestParam Long userId
    ) {
        return ResponseEntity.ok(
                submissionService.createSubmission(request, userId)
        );
    }
}
