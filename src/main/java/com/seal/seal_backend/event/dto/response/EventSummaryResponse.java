package com.seal.seal_backend.event.dto.response;

import com.seal.seal_backend.domain.entity.Event;
import com.seal.seal_backend.domain.enums.EventStatus;
import com.seal.seal_backend.domain.enums.EventType;
import java.time.LocalDateTime;

public record EventSummaryResponse(
        Long id,
        String name,
        String slug,
        EventType eventType,
        EventStatus status,
        LocalDateTime registrationStart,
        LocalDateTime registrationEnd
) {
    public static EventSummaryResponse from(Event e) {
        return new EventSummaryResponse(
                e.getId(), e.getName(), e.getSlug(), e.getEventType(),
                e.getStatus(), e.getRegistrationStart(), e.getRegistrationEnd()
        );
    }
}
