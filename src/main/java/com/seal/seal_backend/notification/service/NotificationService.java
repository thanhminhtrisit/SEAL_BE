package com.seal.seal_backend.notification.service;

import com.seal.seal_backend.notification.dto.response.NotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** OWNER: M3. In-app (+ optional email) notifications. */
public interface NotificationService {
    void notifyUser(Long recipientId, Long eventId, String type, String title, String message);

    Page<NotificationResponse> getUserNotifications(Long userId, Pageable pageable);

    long getUnreadCount(Long userId);

    void markAsRead(Long notificationId, Long userId);

    void markAllAsRead(Long userId);
}
