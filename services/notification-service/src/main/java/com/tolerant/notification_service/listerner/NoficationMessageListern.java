package com.tolerant.notification_service.listerner;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.tolerant.notification_service.model.NotificationRequest;
import com.tolerant.notification_service.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class NoficationMessageListern {
    
    private final NotificationService notificationService;

    @RabbitListener(queues = "notification.queue")
    public void handleNotificationMessage(NotificationRequest request) {
        log.info("Received notification message from queue for order: {}", request.getOrderId());

        try {
            notificationService.processAsyncNotification(request);
            log.info("Successfully processed async notification for order: {}", request.getOrderId());

        } catch (Exception e) {
            log.error("Failed to process async notification for order: {}, error: {}", request.getOrderId(), e.getMessage());
        }
    }
}
