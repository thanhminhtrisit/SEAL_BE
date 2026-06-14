package com.seal.seal_backend.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity @Table(name = "notifications")
@Getter @Setter @NoArgsConstructor
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "event_id")
    private Event event;
    @Column(name = "notification_type", nullable = false, length = 50) private String notificationType;
    @Column(nullable = false, length = 200) private String title;
    @Lob @Column(nullable = false) private String message;
    @Column(name = "is_read", nullable = false) private Boolean isRead = false;
    @CreationTimestamp @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "read_at") private LocalDateTime readAt;
}
