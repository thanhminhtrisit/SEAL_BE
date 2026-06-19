package com.seal.seal_backend.team.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTeamRequest(
        @NotBlank @Size(max = 150) String name,
        String description,
        @NotNull Long eventId,
        @NotNull Long categoryId
) {}
