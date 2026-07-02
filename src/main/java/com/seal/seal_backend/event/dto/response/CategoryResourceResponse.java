package com.seal.seal_backend.event.dto.response;

import com.seal.seal_backend.domain.entity.CategoryResource;
import com.seal.seal_backend.domain.enums.ResourceType;
import java.time.LocalDateTime;

public record CategoryResourceResponse(
        Long id,
        Long categoryId,
        String label,
        String url,
        ResourceType resourceType,
        LocalDateTime createdAt
) {
    public static CategoryResourceResponse from(CategoryResource r) {
        return new CategoryResourceResponse(
                r.getId(),
                r.getCategory().getId(),
                r.getLabel(),
                r.getUrl(),
                r.getResourceType(),
                r.getCreatedAt()
        );
    }
}
