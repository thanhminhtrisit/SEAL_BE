package com.seal.seal_backend.submission.controller;

import com.seal.seal_backend.auth.security.UserPrincipal;
import com.seal.seal_backend.common.api.ApiResponse;
import com.seal.seal_backend.common.security.CurrentUser;
import com.seal.seal_backend.submission.dto.request.*;
import com.seal.seal_backend.submission.dto.response.SubmissionDetailResponseDTO;
import com.seal.seal_backend.submission.dto.response.SubmissionMyOverviewResponseDTO;
import com.seal.seal_backend.submission.dto.response.SubmissionResponseDTO;
import com.seal.seal_backend.submission.dto.response.SubmissionVersionResponseDTO;
import com.seal.seal_backend.submission.service.SubmissionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
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
    @PostMapping("/drafts")
    public ResponseEntity<ApiResponse<SubmissionDetailResponseDTO>> createDraft(
            @Valid @RequestBody CreateDraftSubmissionRequestDTO request,
            @CurrentUser UserPrincipal user
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(submissionService.createDraft(request, user.getId())));
    }

    @PreAuthorize("hasRole('TEAM_LEADER')")
    @PutMapping("/{id}/draft")
    public ResponseEntity<ApiResponse<SubmissionDetailResponseDTO>> updateDraft(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDraftSubmissionRequestDTO request,
            @CurrentUser UserPrincipal user
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(submissionService.updateDraft(id, request, user.getId()))
        );
    }

    @PreAuthorize("hasRole('TEAM_LEADER')")
    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<SubmissionDetailResponseDTO>> submit(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) SubmitSubmissionRequestDTO request,
            @CurrentUser UserPrincipal user
    ) {
        SubmitSubmissionRequestDTO safeRequest =
                request != null ? request : new SubmitSubmissionRequestDTO();
        return ResponseEntity.ok(
                ApiResponse.ok(submissionService.submit(id, safeRequest, user.getId()))
        );
    }

    @PreAuthorize("hasRole('TEAM_LEADER')")
    @PostMapping("/{id}/resubmit")
    public ResponseEntity<ApiResponse<SubmissionDetailResponseDTO>> resubmit(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) SubmitSubmissionRequestDTO request,
            @CurrentUser UserPrincipal user
    ) {
        SubmitSubmissionRequestDTO safeRequest =
                request != null ? request : new SubmitSubmissionRequestDTO();
        return ResponseEntity.ok(
                ApiResponse.ok(submissionService.resubmit(id, safeRequest, user.getId()))
        );
    }

    @PreAuthorize("hasAnyRole('TEAM_LEADER', 'TEAM_MEMBER', 'COORDINATOR', 'SUPER_COORDINATOR', 'ADMIN')")
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<SubmissionDetailResponseDTO>> getCurrentSubmission(
            @RequestParam Long teamId,
            @RequestParam Long roundId,
            @CurrentUser UserPrincipal user
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(submissionService.getCurrentSubmission(
                        teamId,
                        roundId,
                        user.getId(),
                        isStaffViewer(user)
                ))
        );
    }

    @PreAuthorize("hasAnyRole('TEAM_LEADER', 'TEAM_MEMBER')")
    @GetMapping("/my-overview")
    public ResponseEntity<ApiResponse<SubmissionMyOverviewResponseDTO>> getMyOverview(
            @CurrentUser UserPrincipal user
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(submissionService.getMyOverview(user.getId()))
        );
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

    @PreAuthorize("hasAnyRole('TEAM_LEADER', 'TEAM_MEMBER', 'COORDINATOR', 'SUPER_COORDINATOR', 'ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SubmissionDetailResponseDTO>>
    getSubmission(@PathVariable Long id, @CurrentUser UserPrincipal user) {

        return ResponseEntity.ok(
                ApiResponse.ok(submissionService.getSubmission(
                        id,
                        user.getId(),
                        isStaffViewer(user)
                ))
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

    @PreAuthorize("hasAnyRole('TEAM_LEADER', 'TEAM_MEMBER', 'COORDINATOR', 'SUPER_COORDINATOR', 'ADMIN')")
    @GetMapping("/{id}/versions")
    public ResponseEntity<ApiResponse<List<SubmissionVersionResponseDTO>>>
    getVersions(@PathVariable Long id, @CurrentUser UserPrincipal user) {

        return ResponseEntity.ok(
                ApiResponse.ok(submissionService.getVersions(
                        id,
                        user.getId(),
                        isStaffViewer(user)
                ))
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
            @Valid @RequestBody SelectSubmissionVersionRequestDTO request,
            @CurrentUser UserPrincipal user
    ) {

        submissionService.selectCurrentVersion(
                submissionId,
                request.getVersionId(),
                user.getId()
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

    private boolean isStaffViewer(UserPrincipal user) {
        return user.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_COORDINATOR")
                        || role.equals("ROLE_SUPER_COORDINATOR")
                        || role.equals("ROLE_ADMIN"));
    }
}
