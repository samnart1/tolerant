package com.tolerant.order_service.exception;

public class PaymentFailedException extends RuntimeException {

    public PaymentFailedException(String message) {
        super(message);
    }

    public PaymentFailedException(String message, Throwable cuase) {
        super(message, cuase);
    }
}
