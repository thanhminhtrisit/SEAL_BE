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
    @Operation(summary = "List active disciplines",
               description = "Returns all disciplines with is_active = true, ordered by name.")
    public ApiResponse<List<DisciplineResponse>> listDisciplines() {
        return ApiResponse.ok(governanceQueryService.listActiveDisciplines());
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
