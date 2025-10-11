package com.tolerant.order_service.model;

public enum OrderStatus {
    PENDING,
    PAYMENT_PROCESSING,
    PAYMENT_COMPLETED,
    INVENTORY_RESERVED,
    COMPLETED,
    FAILED,
    CANCELED
}
