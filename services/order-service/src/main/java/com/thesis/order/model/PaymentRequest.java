package com.thesis.order.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentRequest {
    private String orderId;
    private String productId;
    private Integer quantity;
    private String paymentMethod;
    private Double amount;
}
