package com.seal.seal_backend.event.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateCriteriaSetRequest(
        @Size(max = 150) String name,
        String description
) {}
