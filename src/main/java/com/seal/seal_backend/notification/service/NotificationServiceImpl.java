package com.seal.seal_backend.notification.service;

import com.seal.seal_backend.domain.entity.Notification;
import com.seal.seal_backend.domain.entity.User;
import com.seal.seal_backend.domain.repository.NotificationRepository;
import com.seal.seal_backend.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

/** Stub impl — persists in-app notification. Email dispatch is TODO (FR-AWD-02). */
@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notifications;
    private final UserRepository users;

    public NotificationServiceImpl(NotificationRepository notifications, UserRepository users) {
        this.notifications = notifications;
        this.users = users;
    }

    @Override
    public void notifyUser(Long recipientId, Long eventId, String type, String title, String message) {
        User recipient = users.findById(recipientId).orElseThrow();
        Notification n = new Notification();
        n.setRecipient(recipient);
        n.setNotificationType(type);
        n.setTitle(title);
        n.setMessage(message);
        notifications.save(n);
    }
}
