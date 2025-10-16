package com.tolerant.notification_service.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.tolerant.notification_service.model.*;
import com.tolerant.notification_service.repo.NotificationRepository;

import io.micrometer.core.annotation.Timed;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Repository
@RequiredArgsConstructor
@Slf4j
public class NotificationProcessingService implements NotificationService {

    private final NotificationRepository notificationRepo;
    private final Random random = new Random();

    @Value("${chaos.enabled:false}")
    private boolean chaosEnabled;

    @Value("${chaos.failure-rate:0.0}")
    private double chaosFailureRate;

    @Value("${chaos.delay-ms:0}")
    private long chaosDelayMs;

    @Value("${notification.processing.base-delay-ms:50}")
    private long baseProcessingDelayMs;

    @Value("${notification.processing.random-delay-ms:100}")
    private long randomProcessingDelayMs;

    @Override
    @Transactional
    @Timed(value = "notification.send", description = "Time taken to send notification")
    public NotificationResponse sendNotification(NotificationRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Processing sync notification for order: {}, type: {}", request.getOrderId(), request.getNotificationType());

        return processNotification(request, "HTTP", startTime);
    }

    @Override
    @Transactional
    @Timed(value = "notification.async", description = "Time taken to process async notification")
    public NotificationResponse processAsyncNotification(NotificationRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Processing async notification from queue for order: {}, type: {}", request.getOrderId(), request.getNotificationType());

        return processNotification(request, "RABBITMQ", startTime);
    }

    private NotificationResponse processNotification(NotificationRequest request, String channel, LocalDateTime startTime) {
        Notification notification = Notification.builder()
            .orderId(request.getOrderId())
            .customerId(request.getCustomerId())
            .email(request.getEmail())
            .phone(request.getPhone())
            .message(request.getMessage())
            .type(parseNotificationType(request.getNotificationType()))
            .status(NotificationStatus.PROCESSING)
            .channel(channel)
            .build();

        notification = notificationRepo.save(notification);

        try {
            if (chaosEnabled) {
                applyChaos();
            }

            simulateNotificationSending(request);

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());

            log.info("Notification sent successfully. ID: {}, Order: {}, Channel: {}", notification.getNotificationId(), notification.getOrderId(), channel);

        } catch (NotificationException e) {
            log.error("Notification failed for order: {}, reason: {}", request.getOrderId(), e.getMessage());
            notification.setStatus(NotificationStatus.FAILED);
            notification.setFailureReason(e.getMessage());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Notification processing interrupted for order: {}", request.getOrderId());
            notification.setStatus(NotificationStatus.FAILED);
            notification.setFailureReason("Processing interrupted");
        }

        notification.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        notification = notificationRepo.save(notification);

        return NotificationResponse.builder()
            .notificationId(notification.getNotificationId())
            .status(notification.getStatus().name())
            .message(notification.getStatus() == NotificationStatus.SENT
                ? "Notification sent successfully"
                : "Notification failed: " + notification.getFailureReason())
            .processingTimeMs(notification.getProcessingTimeMs())
            .channel(channel)
            .build();
    }

    @Override
    public Notification getNotification(String notificationId) {
        return notificationRepo.findByNotificationId(notificationId)
            .orElseThrow(() -> new NotificationException("Notification not found: " + notificationId));
    }

    @Override
    public List<Notification> getNotificationsByOrder(String orderId) {
        return notificationRepo.findByOrderId(orderId);
    }

    @Override
    public List<Notification> getNotificationsByCustomer(String customerId) {
        return notificationRepo.findByCustomerId(customerId);
    }

    @Override
    public List<Notification> getAllNotifications() {
        return notificationRepo.findAll();
    }

    private void simulateNotificationSending(NotificationRequest request) throws InterruptedException {
        long delay = baseProcessingDelayMs + random.nextInt((int) randomProcessingDelayMs);
        log.debug("Simulating notification sending delay: {}ms", delay);
        Thread.sleep(delay);

        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            log.debug("Sending email to: {}", request.getEmail());
        }
        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            log.debug("Sending SMS to: {}", request.getPhone());
        }
    }

    private NotificationType parseNotificationType(String type) {
        try {
            return NotificationType.valueOf(type.toUpperCase());

        } catch(IllegalArgumentException e) {
            log.warn("Unknown notification type: {}, defaulting to GENERAL", type);
            return NotificationType.GENERAL;
        }
    }

    private void applyChaos() {
        if (chaosDelayMs > 0) {
            try {
                log.debug("Applying dhaos delay: {}ms", chaosDelayMs);
                Thread.sleep(chaosDelayMs);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (chaosFailureRate > 0 && random.nextDouble() < chaosFailureRate) {
            log.warn("Chaos: Injecting notification failure");
            throw new NotificationException("Chaos engineering: Random notification failure");
        }
    }

    @Data
    @AllArgsConstructor
    public static class ChaosConfig {
        private boolean enabled;
        private double failureRate;
        private long delayMs;
    }

    public ChaosConfig getChaosConfig() {
        return new ChaosConfig(chaosEnabled, chaosFailureRate, chaosDelayMs);
    }
    
}
