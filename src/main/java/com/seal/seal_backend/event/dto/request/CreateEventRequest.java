package com.seal.seal_backend.event.dto.request;

import com.seal.seal_backend.domain.enums.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record CreateEventRequest(
        @NotBlank @Size(max = 200) String name,
        @NotNull Long disciplineId,
        @NotNull Long termPlanId,
        @NotNull EventType eventType,
        String description,
        LocalDateTime registrationStart,
        LocalDateTime registrationEnd
) {}
