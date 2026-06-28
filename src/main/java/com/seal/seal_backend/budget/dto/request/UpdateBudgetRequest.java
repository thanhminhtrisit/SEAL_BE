package com.seal.seal_backend.budget.dto.request;

import com.seal.seal_backend.domain.enums.BudgetStatus;
import jakarta.validation.constraints.Size;

public record UpdateBudgetRequest(
        @Size(max = 10) String currency,
        BudgetStatus status
) {}
