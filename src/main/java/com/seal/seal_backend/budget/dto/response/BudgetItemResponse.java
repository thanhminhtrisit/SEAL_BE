package com.seal.seal_backend.budget.dto.response;

import com.seal.seal_backend.domain.entity.BudgetItem;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BudgetItemResponse(
        Long id,
        Long categoryId,
        String categoryName,
        String description,
        BigDecimal quantity,
        BigDecimal unitCost,
        BigDecimal amount,
        String notes,
        LocalDateTime createdAt
) {
    public static BudgetItemResponse from(BudgetItem item) {
        return new BudgetItemResponse(
                item.getId(),
                item.getCategory().getId(),
                item.getCategory().getName(),
                item.getDescription(),
                item.getQuantity(),
                item.getUnitCost(),
                item.getAmount(),
                item.getNotes(),
                item.getCreatedAt());
    }
}
