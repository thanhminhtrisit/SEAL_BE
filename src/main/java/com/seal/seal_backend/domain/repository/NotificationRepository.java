package com.seal.seal_backend.domain.repository;

import com.seal.seal_backend.domain.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Lấy danh sách thông báo của một user, sắp xếp mới nhất lên đầu
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    // Đếm số lượng thông báo chưa đọc
    long countByRecipientIdAndIsReadFalse(Long recipientId);

    // Lấy tất cả thông báo chưa đọc của user (phục vụ cho việc mark all as read)
    List<Notification> findByRecipientIdAndIsReadFalse(Long recipientId);
}
