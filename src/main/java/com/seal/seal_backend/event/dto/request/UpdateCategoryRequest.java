package com.seal.seal_backend.event.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
        @Size(max = 150) String name,
        String description,
        Long mentorId,
        Boolean active
) {}
