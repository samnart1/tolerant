package com.tolerant.notification_service.repo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tolerant.notification_service.model.Notification;
import com.tolerant.notification_service.model.NotificationStatus;
import com.tolerant.notification_service.model.NotificationType;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Optional<Notification> findByNotificationId(String notificationId);
    List<Notification> findByOrderId(String orderId);
    List<Notification> findByCustomerId(String customerId);
    List<Notification> findByStatus(NotificationStatus status);
    List<Notification> findByType(NotificationType type);
    List<Notification> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    long countByStatus(NotificationStatus status);
    long countByChannel(String channel);
}
