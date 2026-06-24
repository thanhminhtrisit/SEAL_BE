package com.seal.seal_backend.submission.controller;

import com.seal.seal_backend.common.api.ApiResponse;
import com.seal.seal_backend.submission.dto.request.*;
import com.seal.seal_backend.submission.dto.response.SubmissionDetailResponseDTO;
import com.seal.seal_backend.submission.dto.response.SubmissionResponseDTO;
import com.seal.seal_backend.submission.dto.response.SubmissionVersionResponseDTO;
import com.seal.seal_backend.submission.service.SubmissionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Flow: Project submission tracking
 * OWNER: M2 — Lê Quang Hải
 * Only the owner edits files under the 'submission' package. Put authorization at method level, e.g.
 *   @org.springframework.security.access.prepost.PreAuthorize("hasRole('COORDINATOR')")
 */
@RestController
@RequestMapping("/api/submissions")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Submission Tracking")
@RequiredArgsConstructor
public class SubmissionController {

    @Autowired
    private final SubmissionService submissionService;

    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.ok("Submission Tracking module is alive");
    }

    @PreAuthorize("hasRole('TEAM_LEADER')")
    @PostMapping
    public ResponseEntity<SubmissionResponseDTO> createSubmission(
            @RequestBody CreateSubmissionRequestDTO request,
            @RequestParam Long userId
    ) {
        return ResponseEntity.ok(
                submissionService.createSubmission(request, userId)
        );
    }

    @PreAuthorize("hasAnyRole('TEAM_LEADER', 'TEAM_MEMBER')")
    @GetMapping("/{id}")
    public ResponseEntity<SubmissionDetailResponseDTO>
    getSubmission(@PathVariable Long id) {

        return ResponseEntity.ok(
                submissionService.getSubmission(id)
        );
    }

    @PreAuthorize("hasAnyRole('TEAM_LEADER', 'TEAM_MEMBER')")
    @GetMapping("/{id}/current-version")
    public ResponseEntity<SubmissionVersionResponseDTO>
    getCurrentVersion(@PathVariable Long id) {

        return ResponseEntity.ok(
                submissionService.getCurrentVersion(id)
        );
    }

    @PreAuthorize("hasAnyRole('TEAM_LEADER', 'TEAM_MEMBER')")
    @GetMapping("/{id}/versions")
    public ResponseEntity<List<SubmissionVersionResponseDTO>>
    getVersions(@PathVariable Long id) {

        return ResponseEntity.ok(
                submissionService.getVersions(id)
        );
    }

    @PreAuthorize("hasRole('TEAM_LEADER')")
    @PostMapping("/{id}/versions")
    public ResponseEntity<SubmissionVersionResponseDTO>
    createVersion(
            @PathVariable Long id,
            @RequestBody CreateVersionRequestDTO request,
            @RequestParam Long userId
    ) {

        return ResponseEntity.ok(
                submissionService.createVersion(
                        id,
                        request,
                        userId
                )
        );
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable Long id,
            @RequestBody UpdateStatusRequestDTO request
    ) {

        submissionService.updateStatus(
                id,
                request.getStatus()
        );

        return ResponseEntity.ok().build();
    }


    @PutMapping("/versions/{versionId}")
    public ResponseEntity<SubmissionVersionResponseDTO>
    updateVersion(
            @PathVariable Long versionId,
            @RequestBody UpdateSubmissionVersionRequestDTO request
    ) {

        return ResponseEntity.ok(
                submissionService.updateVersion(
                        versionId,
                        request
                )
        );
    }

    @PreAuthorize("hasRole('TEAM_LEADER')")
    @PatchMapping("/{submissionId}/select-version")
    public ResponseEntity<Void> selectVersion(
            @PathVariable Long submissionId,
            @RequestBody SelectSubmissionVersionRequestDTO request
    ) {

        submissionService.selectCurrentVersion(
                submissionId,
                request.getVersionId()
        );

        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<SubmissionDetailResponseDTO>>
    getAllSubmissions() {

        return ResponseEntity.ok(
                submissionService.getAllSubmissions()
        );
    }
}
