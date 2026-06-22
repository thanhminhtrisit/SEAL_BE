package com.seal.seal_backend.budget.controller;

import com.seal.seal_backend.budget.dto.request.CreateBudgetItemRequest;
import com.seal.seal_backend.budget.dto.request.CreateBudgetRequest;
import com.seal.seal_backend.budget.dto.request.UpdateBudgetItemRequest;
import com.seal.seal_backend.budget.dto.response.BudgetCategoryResponse;
import com.seal.seal_backend.budget.dto.response.BudgetResponse;
import com.seal.seal_backend.budget.service.BudgetService;
import com.seal.seal_backend.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Budget Management", description = "FR-BGT-01..04 — event budget and items CRUD")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    // ─── Budget ───────────────────────────────────────────────────────────────

    @PostMapping("/api/events/{eventId}/budget")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Create budget for event (FR-BGT-01)", description = "Max 1 budget per event. Defaults to VND.")
    public ResponseEntity<ApiResponse<BudgetResponse>> createBudget(
            @PathVariable Long eventId,
            @Valid @RequestBody CreateBudgetRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(budgetService.createBudget(eventId, req)));
    }

    @GetMapping("/api/events/{eventId}/budget")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get budget with items and total (FR-BGT-01)")
    public ApiResponse<BudgetResponse> getBudget(@PathVariable Long eventId) {
        return ApiResponse.ok(budgetService.getBudget(eventId));
    }

    // ─── Budget Items ─────────────────────────────────────────────────────────

    @PostMapping("/api/events/{eventId}/budget/items")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Add item to budget (FR-BGT-02)")
    public ResponseEntity<ApiResponse<BudgetResponse>> addItem(
            @PathVariable Long eventId,
            @Valid @RequestBody CreateBudgetItemRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(budgetService.addItem(eventId, req)));
    }

    @PutMapping("/api/events/{eventId}/budget/items/{itemId}")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Update budget item (FR-BGT-03)")
    public ApiResponse<BudgetResponse> updateItem(
            @PathVariable Long eventId,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateBudgetItemRequest req) {
        return ApiResponse.ok(budgetService.updateItem(eventId, itemId, req));
    }

    @DeleteMapping("/api/events/{eventId}/budget/items/{itemId}")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Delete budget item (FR-BGT-04)")
    public ApiResponse<BudgetResponse> deleteItem(
            @PathVariable Long eventId,
            @PathVariable Long itemId) {
        return ApiResponse.ok(budgetService.deleteItem(eventId, itemId));
    }

    // ─── Budget Categories ────────────────────────────────────────────────────

    @GetMapping("/api/budget-categories")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List budget categories for item selection")
    public ApiResponse<List<BudgetCategoryResponse>> listCategories() {
        return ApiResponse.ok(budgetService.listBudgetCategories());
    }
}
