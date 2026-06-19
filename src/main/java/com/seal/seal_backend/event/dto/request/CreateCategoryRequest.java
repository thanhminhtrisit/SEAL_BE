package com.seal.seal_backend.event.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @NotBlank @Size(max = 150) String name,
        String description,
        Long mentorId
) {}
