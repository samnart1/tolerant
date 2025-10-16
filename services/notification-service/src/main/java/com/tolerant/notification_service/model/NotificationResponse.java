package com.tolerant.notification_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationResponse {
    private String notificationId;
    private String status;
    private String message;
    private Long processingTimeMs;
    private String channel;
}
