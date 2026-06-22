package com.seal.seal_backend.team.controller;

import com.seal.seal_backend.auth.security.UserPrincipal;
import com.seal.seal_backend.common.api.ApiResponse;
import com.seal.seal_backend.common.security.CurrentUser;
import com.seal.seal_backend.team.dto.request.*;
import com.seal.seal_backend.team.dto.response.*;
import com.seal.seal_backend.team.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
@Tag(name = "Team & Account Approval", description = "FR-TEAM-01/03/04/05 — team registration and approval")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.ok("Team & Account Approval module is alive");
    }

    // ─── Team CRUD ────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('TEAM_MEMBER') or hasRole('COORDINATOR') or hasRole('TEAM_LEADER')")
    @Operation(summary = "Create team and register to category (FR-TEAM-01)")
    public ResponseEntity<ApiResponse<TeamResponse>> createTeam(
            @Valid @RequestBody CreateTeamRequest req,
            @CurrentUser UserPrincipal user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(teamService.createTeam(req, user.getId())));
    }

    @GetMapping("/{teamId}")
    @Operation(summary = "Get team details")
    public ApiResponse<TeamResponse> getTeam(@PathVariable Long teamId) {
        return ApiResponse.ok(teamService.getTeam(teamId));
    }

    @GetMapping("/by-event/{eventId}")
    @Operation(summary = "List all teams for an event")
    public ApiResponse<List<TeamSummaryResponse>> listByEvent(@PathVariable Long eventId) {
        return ApiResponse.ok(teamService.listTeamsByEvent(eventId));
    }

    // ─── Invitation (FR-TEAM-03) ──────────────────────────────────────────────

    @PostMapping("/{teamId}/invitations")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Invite member by email (BR-TEAM-06: no duplicate in same event)")
    public ResponseEntity<ApiResponse<InvitationResponse>> invite(
            @PathVariable Long teamId,
            @Valid @RequestBody InviteMemberRequest req,
            @CurrentUser UserPrincipal user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(teamService.inviteMember(teamId, req, user.getId())));
    }

    @GetMapping("/{teamId}/invitations")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List invitations for a team — accessible by team members and coordinators only")
    public ApiResponse<List<InvitationResponse>> listInvitations(
            @PathVariable Long teamId,
            @CurrentUser UserPrincipal user) {
        return ApiResponse.ok(teamService.listInvitations(teamId, user.getId(), user.getRoleCode()));
    }

    @GetMapping("/invitations/mine")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List my pending invitations (sent to caller's email)")
    public ApiResponse<List<MyInvitationResponse>> listMyInvitations(
            @CurrentUser UserPrincipal user) {
        return ApiResponse.ok(teamService.listMyInvitations(user.getEmail()));
    }

    @PostMapping("/invitations/{invitationId}/accept")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Accept an invitation — joins team as MEMBER (BR-TEAM-01/02)")
    public ApiResponse<TeamResponse> acceptInvitation(
            @PathVariable Long invitationId,
            @CurrentUser UserPrincipal user) {
        return ApiResponse.ok(teamService.acceptInvitation(invitationId, user.getId()));
    }

    @PostMapping("/invitations/{invitationId}/decline")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Decline an invitation")
    public ApiResponse<InvitationResponse> declineInvitation(
            @PathVariable Long invitationId,
            @CurrentUser UserPrincipal user) {
        return ApiResponse.ok(teamService.declineInvitation(invitationId, user.getId()));
    }

    // ─── Category registration (FR-TEAM-04) ──────────────────────────────────

    @PutMapping("/{teamId}/category")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Set/change team category (BR-TEAM-04: within registration window)")
    public ApiResponse<TeamResponse> registerCategory(
            @PathVariable Long teamId,
            @Valid @RequestBody RegisterTeamCategoryRequest req,
            @CurrentUser UserPrincipal user) {
        return ApiResponse.ok(teamService.registerCategory(teamId, req, user.getId()));
    }

    // ─── Approval (FR-TEAM-05) ────────────────────────────────────────────────

    @PostMapping("/{teamId}/review")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Approve or reject team (FR-TEAM-05, BR-TEAM-01 size check on approve)")
    public ApiResponse<TeamResponse> reviewTeam(
            @PathVariable Long teamId,
            @Valid @RequestBody ApproveTeamRequest req,
            @CurrentUser UserPrincipal user,
            HttpServletRequest httpRequest) {
        return ApiResponse.ok(teamService.reviewTeam(teamId, req, user.getId(), httpRequest.getRemoteAddr()));
    }

    // ─── Remove member (FR-TEAM-07) ───────────────────────────────────────────

    @DeleteMapping("/{teamId}/members/{targetUserId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Remove a member from team (leader or self; leader cannot be removed)")
    public ApiResponse<TeamResponse> removeMember(
            @PathVariable Long teamId,
            @PathVariable Long targetUserId,
            @CurrentUser UserPrincipal user) {
        return ApiResponse.ok(teamService.removeMember(teamId, targetUserId, user.getId()));
    }
}
