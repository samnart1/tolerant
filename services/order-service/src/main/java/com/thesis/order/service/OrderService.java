package com.thesis.order.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.thesis.order.client.InventoryClient;
import com.thesis.order.client.PaymentPublisher;
import com.thesis.order.model.InventoryResponse;
import com.thesis.order.model.Order;
import com.thesis.order.model.OrderRequest;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
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

    @CircuitBreaker(name = "orderService", fallbackMethod = "createOrderFallback")
    @Retry(name = "orderService")
    @Bulkhead(name = "orderService")
    public Order createOrder(OrderRequest request) {
        String orderId = UUID.randomUUID().toString();

        log.info("Creating order {} for product {}", orderId, request.getProductId());

        //check inventory
        InventoryResponse inventoryResponse = inventoryClient.checkInventory(request.getProductId(), request.getQuantity());

        if (!inventoryResponse.isAvailable()) {
            log.warn("Insufficient inventory for product {}", request.getProductId());
            throw new RuntimeException("Insufficient inventory");
        }

        //reserve inventory
        inventoryClient.reserveInventory(request.getProductId(), request.getQuantity());

        //create order
        Order order = Order.builder()
            .id(orderId)
            .productId(request.getProductId())
            .quantity(request.getQuantity())
            .paymentMethod(request.getPaymentMethod())
            .status("PENDING")
            .createdAt(LocalDateTime.now())
            .build();

        orderStore.put(orderId, order);

        //publish payment request asynchonously
        try {
            paymentPublisher.publishPaymentRequest(order);
            log.info("Payment request for order {}", orderId);

        } catch (Exception e) {
            log.error("Failed to publish payment request: {}", e.getMessage());
        }

        orderSuccessCounter.increment();
        return order;
    }

    private Order createOrderFallback(OrderRequest request, Exception e) {
        log.error("Order creation failed, executing fallback: {}", e.getMessage());
        orderFailureCounter.increment();    

        String orderId = UUID.randomUUID().toString();
        Order order = Order.builder()
            .id(orderId)
            .productId(request.getProductId())
            .quantity(request.getQuantity())
            .status("FAILED")
            .createdAt(LocalDateTime.now())
            .build();

        orderStore.put(orderId, order);
        return order;
    }

    public Optional<Order> getOrder(String orderId) {
        return Optional.ofNullable(orderStore.get(orderId));
    }

    public void updateOrderStatus(String orderId, String status) {
        Order order = orderStore.get(orderId);
        if (order != null) {
            order.setStatus(status);
            log.info("Order {} status updated to {}", orderId, status);
        }
    }
}

