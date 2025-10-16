package com.tolerant.notification_service.service;

import java.util.List;

import com.tolerant.notification_service.model.Notification;
import com.tolerant.notification_service.model.NotificationRequest;
import com.tolerant.notification_service.model.NotificationResponse;

public interface NotificationService {
    NotificationResponse sendNotification(NotificationRequest request);
    NotificationResponse processAsyncNotification(NotificationRequest request);
    Notification getNotification(String notificationId);
    List<Notification> getNotificationsByOrder(String orderId);
    List<Notification> getNotificationsByCustomer(String customerId);
    List<Notification> getAllNotifications();
}
