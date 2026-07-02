package com.seal.seal_backend.event.dto.request;

import com.seal.seal_backend.domain.enums.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCategoryResourceRequest(
        @Size(max = 150) String label,
        @NotBlank @Size(max = 500) String url,
        @NotNull ResourceType resourceType
) {}
