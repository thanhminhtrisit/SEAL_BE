package com.seal.seal_backend.event.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record CreateRoundRequest(
        @NotBlank @Size(max = 150) String name,
        @NotNull @Min(1) Integer orderNumber,
        LocalDateTime submissionDeadline,
        LocalDateTime scoringDeadline,
        Integer promotionTopN,
        boolean finalRound,
        Boolean requiresRepo,
        Boolean requiresDemo,
        Boolean requiresSlide,
        Boolean requiresReport
) {}
