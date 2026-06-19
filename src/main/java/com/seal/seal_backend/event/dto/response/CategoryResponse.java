package com.seal.seal_backend.event.dto.response;

import com.seal.seal_backend.domain.entity.Category;
import java.time.LocalDateTime;

public record CategoryResponse(
        Long id,
        Long eventId,
        String name,
        String description,
        Long mentorId,
        String mentorName,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CategoryResponse from(Category c) {
        return new CategoryResponse(
                c.getId(),
                c.getEvent().getId(),
                c.getName(),
                c.getDescription(),
                c.getMentor() != null ? c.getMentor().getId() : null,
                c.getMentor() != null ? c.getMentor().getFullName() : null,
                c.getIsActive(),
                c.getCreatedAt(),
                c.getUpdatedAt());
    }
}
