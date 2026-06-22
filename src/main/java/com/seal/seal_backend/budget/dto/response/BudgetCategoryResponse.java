package com.seal.seal_backend.budget.dto.response;

import com.seal.seal_backend.domain.entity.BudgetCategory;

public record BudgetCategoryResponse(Long id, String code, String name, String description) {
    public static BudgetCategoryResponse from(BudgetCategory c) {
        return new BudgetCategoryResponse(c.getId(), c.getCode(), c.getName(), c.getDescription());
    }
}
