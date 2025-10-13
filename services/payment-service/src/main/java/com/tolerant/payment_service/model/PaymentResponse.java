package com.tolerant.payment_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponse {
    private String paymentId;
    private String status;
    private String message;
    private Long processingTimeMs;
    private String tractionId;
}
