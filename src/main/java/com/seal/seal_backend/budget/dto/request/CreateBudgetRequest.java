package com.seal.seal_backend.budget.dto.request;

import jakarta.validation.constraints.Size;

public record CreateBudgetRequest(
        @Size(max = 10) String currency
) {}
