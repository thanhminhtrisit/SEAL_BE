package com.seal.seal_backend.team.dto.request;

import jakarta.validation.constraints.NotNull;

public record RegisterTeamCategoryRequest(
        @NotNull Long categoryId
) {}
