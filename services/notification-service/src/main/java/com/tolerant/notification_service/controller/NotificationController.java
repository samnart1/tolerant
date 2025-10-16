package com.tolerant.notification_service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tolerant.notification_service.model.Notification;
import com.tolerant.notification_service.model.NotificationRequest;
import com.tolerant.notification_service.model.NotificationResponse;
import com.tolerant.notification_service.service.NotificationService;

import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {
    
    private final NotificationService notificationService;

    @PostMapping("/send")
    @Timed(value = "notification.send.api", description = "Time taken for notification API call")
    public ResponseEntity<NotificationResponse> sendNotification(@Valid @RequestBody NotificationRequest request) {
        log.info("Received notification request for order: {}", request.getOrderId());
        NotificationResponse response = notificationService.sendNotification(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{notificationId}")
    @Timed(value = "notification.get", description = "Time taken to get notification")
    public ResponseEntity<Notification> getNotification(String notificationId) {
        log.info("Get notification: ", notificationId);
        Notification notification = notificationService.getNotification(notificationId);
        return new ResponseEntity<>(notification, HttpStatus.OK);
    }

    @GetMapping("/customer/{customerId}")
    @Timed(value = "notification.getByCustomer", description = "Time taken to get notification by customer")
    public ResponseEntity<List<Notification>> getNotificationsByCustomer(String customerId) {
        log.info("Get notification by cutomer: {}", customerId);
        List<Notification> notifications = notificationService.getNotificationsByCustomer(customerId);
        return new ResponseEntity<>(notifications, HttpStatus.OK);
    }

    @GetMapping("/orders/{orderId}")
    @Timed(value = "notification.getByOrder", description = "Time taken to get notification by order")
    public ResponseEntity<List<Notification>> getNotificationsByOrder(String orderId) {
        log.info("Get notifications by order: {}", orderId);
        List<Notification> notifications = notificationService.getNotificationsByOrder(orderId);
        return new ResponseEntity<>(notifications, HttpStatus.OK);
    }

    @GetMapping
    @Timed(value = "notifications.getAll", description = "Time taken to get all notifications")
    public ResponseEntity<List<Notification>> getAllNotifications() {
        log.info("Get all notifications");
        List<Notification> notifications = notificationService.getAllNotifications();
        return new ResponseEntity<>(notifications, HttpStatus.OK);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return new ResponseEntity<>("Notification service is up and running healthily!", HttpStatus.OK);
    }
}
