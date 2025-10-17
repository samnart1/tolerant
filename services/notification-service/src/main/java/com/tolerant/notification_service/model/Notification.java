package com.tolerant.notification_service.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String notificationId;

    @Column(nullable = false)
    private String orderId;

    @Column(nullable = false)
    private String customerId;

    private String email;

    private String phone;

    @Column(nullable = false, length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    @Column(nullable = false)
    private String channel; // for http or rabbitmq communication

    @Column(nullable = false)
    private String failureReason;

    // @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime sentAt;

    // @Column(nullable = false)
    private Long processingTimeMs;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (notificationId == null) {
            notificationId = "NOTIF-" + System.currentTimeMillis();
        }
    }
}
