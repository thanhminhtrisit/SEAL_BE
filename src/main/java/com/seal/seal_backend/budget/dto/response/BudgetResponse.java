package com.seal.seal_backend.budget.dto.response;

import com.seal.seal_backend.domain.entity.EventBudget;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BudgetResponse(
        Long id,
        Long eventId,
        String currency,
        BigDecimal totalEstimatedCost,
        String status,
        List<BudgetItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static BudgetResponse from(EventBudget b, List<BudgetItemResponse> items) {
        return new BudgetResponse(
                b.getId(),
                b.getEvent().getId(),
                b.getCurrency(),
                b.getTotalEstimatedCost(),
                b.getStatus().name(),
                items,
                b.getCreatedAt(),
                b.getUpdatedAt());
    }
}
