package com.tolerant.order_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationRequest {
    private String orderId;
    private String customerId;
    private String email;
    private String message;
    private String phone;
    private String notificationType;
}
