package com.thesis.order.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderResponse {
    private String orderId;
    private String status;
    private String message;
}
