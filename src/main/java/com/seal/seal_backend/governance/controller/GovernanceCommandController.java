package com.seal.seal_backend.governance.controller;

import com.seal.seal_backend.auth.security.UserPrincipal;
import com.seal.seal_backend.common.api.ApiResponse;
import com.seal.seal_backend.common.security.CurrentUser;
import com.seal.seal_backend.governance.dto.request.CreateDisciplineRequest;
import com.seal.seal_backend.governance.dto.request.CreateTermPlanRequest;
import com.seal.seal_backend.governance.dto.request.UpdateDisciplineRequest;
import com.seal.seal_backend.governance.dto.request.UpdateTermPlanRequest;
import com.seal.seal_backend.governance.dto.response.DisciplineResponse;
import com.seal.seal_backend.governance.dto.response.TermPlanResponse;
import com.seal.seal_backend.governance.service.GovernanceCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** FR-GOV-01 / FR-GOV-02 — Super-Coordinator writes for disciplines and term quotas. */
@RestController
@Tag(name = "Governance — Command", description = "Super-Coordinator writes for disciplines and term quotas")
@RequiredArgsConstructor
public class GovernanceCommandController {

    private final GovernanceCommandService governanceCommandService;

    @PostMapping("/api/disciplines")
    @PreAuthorize("hasRole('SUPER_COORDINATOR')")
    @Operation(summary = "Create a discipline (FR-GOV-01)")
    public ApiResponse<DisciplineResponse> createDiscipline(
            @RequestBody CreateDisciplineRequest req, @CurrentUser UserPrincipal user) {
        return ApiResponse.ok(governanceCommandService.createDiscipline(req, user.getId()));
    }

    @PatchMapping("/api/disciplines/{id}")
    @PreAuthorize("hasRole('SUPER_COORDINATOR')")
    @Operation(summary = "Rename / edit / activate-deactivate a discipline (FR-GOV-01)")
    public ApiResponse<DisciplineResponse> updateDiscipline(
            @PathVariable Long id, @RequestBody UpdateDisciplineRequest req, @CurrentUser UserPrincipal user) {
        return ApiResponse.ok(governanceCommandService.updateDiscipline(id, req, user.getId()));
    }

    @PostMapping("/api/term-plans")
    @PreAuthorize("hasRole('SUPER_COORDINATOR')")
    @Operation(summary = "Set a term quota (FR-GOV-02)")
    public ApiResponse<TermPlanResponse> createTermPlan(
            @RequestBody CreateTermPlanRequest req, @CurrentUser UserPrincipal user) {
        return ApiResponse.ok(governanceCommandService.createTermPlan(req, user.getId()));
    }

    @PatchMapping("/api/term-plans/{id}")
    @PreAuthorize("hasRole('SUPER_COORDINATOR')")
    @Operation(summary = "Update a term quota's max events (FR-GOV-02)")
    public ApiResponse<TermPlanResponse> updateTermPlan(
            @PathVariable Long id, @RequestBody UpdateTermPlanRequest req, @CurrentUser UserPrincipal user) {
        return ApiResponse.ok(governanceCommandService.updateTermPlan(id, req, user.getId()));
    }
}
