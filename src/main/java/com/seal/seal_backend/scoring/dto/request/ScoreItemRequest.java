package com.seal.seal_backend.scoring.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ScoreItemRequest {

    @NotNull(message = "Criterion id is required")
    private Long criterionId;

    @NotNull(message = "Score value is required")
    @DecimalMin(value = "0.00", message = "Score value must be greater than or equal to 0")
    private BigDecimal scoreValue;

    private String comment;
}