package com.seal.seal_backend.team.dto.request;

import jakarta.validation.constraints.NotNull;

public record ApproveTeamRequest(
        @NotNull boolean approved,
        String reason
) {}
