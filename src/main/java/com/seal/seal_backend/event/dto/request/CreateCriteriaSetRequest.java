package com.seal.seal_backend.event.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateCriteriaSetRequest(
        @NotBlank @Size(max = 150) String name,
        String description,
        Long roundId,
        @NotEmpty List<@Valid AddCriterionRequest> criteria
) {}
