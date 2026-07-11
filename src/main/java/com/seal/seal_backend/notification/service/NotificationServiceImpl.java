package com.seal.seal_backend.notification.service;

import com.seal.seal_backend.domain.entity.Event;
import com.seal.seal_backend.domain.entity.Notification;
import com.seal.seal_backend.domain.entity.User;
import com.seal.seal_backend.domain.repository.EventRepository;
import com.seal.seal_backend.domain.repository.NotificationRepository;
import com.seal.seal_backend.domain.repository.UserRepository;
import com.seal.seal_backend.notification.dto.response.NotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notifications;
    private final UserRepository users;
    private final EventRepository events; // Cần thêm để set Event Entity

    public NotificationServiceImpl(NotificationRepository notifications, UserRepository users, EventRepository events) {
        this.notifications = notifications;
        this.users = users;
        this.events = events;
    }

    // Tạo và lưu thông báo xuống database
    @Override
    @Transactional
    public void notifyUser(Long recipientId, Long eventId, String type, String title, String message) {
        User recipient = users.findById(recipientId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Notification n = new Notification();
        n.setRecipient(recipient);
        n.setNotificationType(type);
        n.setTitle(title);
        n.setMessage(message);

        // Map event nếu có truyền eventId
        if (eventId != null) {
            Event event = events.findById(eventId).orElse(null);
            n.setEvent(event);
        }

        notifications.save(n); //[cite: 5]
    }

    // Lấy danh sách thông báo(DTO) để đưa lên FE
    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(Long userId, Pageable pageable) {
        return notifications.findByRecipientIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToResponse);
    }

    // Đếm số lượng thông báo chưa đọc
    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notifications.countByRecipientIdAndIsReadFalse(userId);
    }

    // Cập nhật trạng thái thông báo và lưu thời gian đã đọc
    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notifications.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        // Đảm bảo user chỉ có thể đánh dấu đọc thông báo của chính mình
        if (!notification.getRecipient().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to notification");
        }

        if (!notification.getIsRead()) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notifications.save(notification);
        }
    }

    // Đánh dấu đã đọc cho mọi tin nhắn
    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unreadList = notifications.findByRecipientIdAndIsReadFalse(userId);
        LocalDateTime now = LocalDateTime.now();

        unreadList.forEach(notification -> {
            notification.setIsRead(true);
            notification.setReadAt(now);
        });

        notifications.saveAll(unreadList);
    }

    // Helper map từ Entity sang DTO
    private NotificationResponse mapToResponse(Notification n) {
        Long eventId = n.getEvent() != null ? n.getEvent().getId() : null;
        return new NotificationResponse(
                n.getId(),
                eventId,
                n.getNotificationType(),
                n.getTitle(),
                n.getMessage(),
                n.getIsRead(),
                n.getCreatedAt()
        );
    }

    @Override
    @Async // Đẩy hàm này chạy ngầm (Background thread) để không làm chậm API Publish
    @Transactional
    public void notifyUsersBatch(List<Long> recipientIds, Long eventId, String type, String title, String message) {
        if (recipientIds == null || recipientIds.isEmpty()) return;

        // Dùng getReferenceById thay vì findById để tạo Proxy (chỉ lấy ID, không gọi DB)
        Event eventRef = null;
        if (eventId != null) {
            eventRef = events.getReferenceById(eventId);
        }

        Event finalEventRef = eventRef;

        // Tạo danh sách thông báo
        List<Notification> notificationList = recipientIds.stream().map(userId -> {
            Notification n = new Notification();
            // Dùng getReferenceById cho User để tránh N câu SELECT User
            n.setRecipient(users.getReferenceById(userId));
            n.setEvent(finalEventRef);
            n.setNotificationType(type);
            n.setTitle(title);
            n.setMessage(message);
            n.setIsRead(false);
            return n;
        }).toList();

        // Dùng saveAll để gom chung thành Batch Insert (Chỉ 1 lệnh gọi DB)
        notifications.saveAll(notificationList);

        System.out.println("Đã gửi thành công " + recipientIds.size() + " thông báo chạy ngầm!");
    }
}