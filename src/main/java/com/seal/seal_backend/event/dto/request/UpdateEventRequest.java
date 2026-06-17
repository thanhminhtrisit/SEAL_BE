package com.seal.seal_backend.event.dto.request;

import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record UpdateEventRequest(
        @Size(max = 200) String name,
        String description,
        LocalDateTime registrationStart,
        LocalDateTime registrationEnd
) {}
