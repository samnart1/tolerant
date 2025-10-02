package com.thesis.order.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.thesis.order.client.InventoryClient;
import com.thesis.order.client.PaymentPublisher;
import com.thesis.order.model.Order;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OrderService {
    
    private final InventoryClient inventoryClient;
    private final PaymentPublisher paymentPublisher;
    private final Map<String, Order> orderStore = new ConcurrentHashMap<>();

    private final Counter orderSuccessCounter;
    private final Counter orderFailureCounter;

    public OrderService(InventoryClient inventoryClient, PaymentPublisher paymentPublisher, MeterRegistry meterRegistry) {
        this.inventoryClient = inventoryClient;
        this.paymentPublisher = paymentPublisher;

        this.orderSuccessCounter = Counter.builder("order.success")
            .description("Number of successful orders")
            .register(meterRegistry);

        this.orderFailureCounter = Counter.builder("order.failure")
            .description("Number of failed orders")
            .register(meterRegistry);
    }
}
