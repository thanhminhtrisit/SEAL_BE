package com.seal.seal_backend.governance.controller;

import com.seal.seal_backend.common.api.ApiResponse;
import com.seal.seal_backend.governance.dto.response.DisciplineResponse;
import com.seal.seal_backend.governance.dto.response.TermPlanResponse;
import com.seal.seal_backend.governance.service.GovernanceQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Governance — Query", description = "Read-only lookups for Disciplines and Term Plans (FR-GOV)")
@RequiredArgsConstructor
public class GovernanceQueryController {

    private final GovernanceQueryService governanceQueryService;

    @GetMapping("/api/disciplines")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List disciplines",
               description = "Active only by default; pass includeInactive=true (Super-Coordinator management) to include inactive ones.")
    public ApiResponse<List<DisciplineResponse>> listDisciplines(
            @RequestParam(required = false, defaultValue = "false") boolean includeInactive) {
        return ApiResponse.ok(governanceQueryService.listDisciplines(includeInactive));
    }

    @GetMapping("/api/term-plans")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List term plans with slot usage",
               description = "Optional filters: disciplineId, year. Returns usedEvents (non-ARCHIVED) and remaining quota.")
    public ApiResponse<List<TermPlanResponse>> listTermPlans(
            @RequestParam(required = false) Long disciplineId,
            @RequestParam(required = false) Integer year) {
        return ApiResponse.ok(governanceQueryService.listTermPlans(disciplineId, year));
    }
}
