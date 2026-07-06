package com.seal.seal_backend.notification.dto.response;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        Long eventId,
        String notificationType,
        String title,
        String message,
        Boolean isRead,
        LocalDateTime createdAt
) {}