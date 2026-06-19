package com.seal.seal_backend.event.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record AddCriterionRequest(
        @NotBlank @Size(max = 150) String name,
        String description,
        @NotNull @DecimalMin("0.01") BigDecimal maxScore,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal weight,
        @NotNull @Min(1) Integer displayOrder
) {}
