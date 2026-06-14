package com.seal.seal_backend.notification.service;

/** OWNER: M3. In-app (+ optional email) notifications. */
public interface NotificationService {
    void notifyUser(Long recipientId, Long eventId, String type, String title, String message);
}
