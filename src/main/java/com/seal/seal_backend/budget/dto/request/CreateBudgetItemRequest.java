package com.seal.seal_backend.budget.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateBudgetItemRequest(
        @NotNull Long categoryId,
        @NotBlank String description,
        @NotNull @DecimalMin(value = "0.01", message = "quantity must be greater than 0") BigDecimal quantity,
        @NotNull @DecimalMin(value = "0", message = "unitCost must be >= 0") BigDecimal unitCost,
        String notes
) {}
