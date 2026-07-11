package com.seal.seal_backend.submission.controller;

import com.seal.seal_backend.auth.security.UserPrincipal;
import com.seal.seal_backend.common.api.ApiResponse;
import com.seal.seal_backend.common.security.CurrentUser;
import com.seal.seal_backend.submission.dto.request.*;
import com.seal.seal_backend.submission.dto.response.SubmissionDetailResponseDTO;
import com.seal.seal_backend.submission.dto.response.SubmissionMyOverviewResponseDTO;
import com.seal.seal_backend.submission.service.SubmissionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    private final SubmissionService submissionService;

    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.ok("Submission Tracking module is alive");
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

    // Coarse gate only: the real "is the active LEADER of this team" check is
    // validateTeamLeader(...) in the service (team_members.member_role = LEADER).
    // users.primary_role is TEAM_MEMBER for every registrant, so hasRole('TEAM_LEADER')
    // would 403 every legitimate leader.
    @PreAuthorize("hasAnyRole('TEAM_LEADER', 'TEAM_MEMBER')")
    @PostMapping
    public ResponseEntity<ApiResponse<SubmissionDetailResponseDTO>> createSubmission(
            @Valid @RequestBody CreateSubmissionRequestDTO request,
            @CurrentUser UserPrincipal user
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(submissionService.createSubmission(request, user.getId())));
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

    @PreAuthorize("hasAnyRole('TEAM_LEADER', 'TEAM_MEMBER', 'COORDINATOR', 'SUPER_COORDINATOR', 'ADMIN')")
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<SubmissionDetailResponseDTO>>> getHistory(
            @RequestParam Long teamId,
            @RequestParam Long roundId,
            @CurrentUser UserPrincipal user) {
        return ResponseEntity.ok(
                ApiResponse.ok(submissionService.getHistory(
                        teamId,
                        roundId,
                        user.getId(),
                        isStaffViewer(user)
                ))
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
