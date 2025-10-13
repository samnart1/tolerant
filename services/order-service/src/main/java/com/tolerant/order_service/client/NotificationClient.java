package com.tolerant.order_service.client;

import java.time.Duration;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import com.tolerant.order_service.model.NotificationRequest;
import com.tolerant.order_service.model.NotificationResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Configuration
@Slf4j
public class NotificationClient {

    private final WebClient.Builder webClientBuilder;
    private final RabbitTemplate rabbitTemplate;

    public Mono<NotificationResponse> sendNotification(NotificationRequest request, Duration timeout) {
        log.info("calling notification service for order: {}", request.getOrderId());

        return webClientBuilder.build()
            .post()
            .uri("http://notification-service/api/notifications/send")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(NotificationResponse.class)
            .timeout(timeout)
            .doOnSuccess(response -> log.info("Notification send: {}", response.getNotificationId()))
            .doOnError(error -> log.info("Notification failed", error.getMessage()));
    }

    public void sendNotificationAsync(NotificationRequest notificationRequest) {
        log.info("Sending notification to queue for order: {}", notificationRequest.getOrderId());
        rabbitTemplate.convertAndSend("notification.exchange", "notification.order", notificationRequest);
    }

    public NotificationResponse sendNotificationSync(NotificationRequest notificationRequest, Duration timeout) {
        return sendNotification(notificationRequest, timeout).block();
    }

}
