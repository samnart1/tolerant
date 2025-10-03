package com.thesis.order.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thesis.order.model.Order;
import com.thesis.order.model.OrderRequest;
import com.thesis.order.model.OrderResponse;
import com.thesis.order.service.OrderService;

import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Timed(value = "order.create", description = "Time taken to create order")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request) {
        log.info("Received order request for product: {}, quantity: {}", request.getProductId(), request.getQuantity());

        try {
            Order order = orderService.createOrder(request);

            OrderResponse response = OrderResponse.builder()
                .orderId(order.getId())
                .status(order.getStatus())
                .message("Order created successfully")
                .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Failed to create order: {}", e.getMessage(), e);

            OrderResponse errorResponse = OrderResponse.builder()
                .status("FAILED")
                .message("Failed to create order: " + e.getMessage())
                .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/{orderId}")
    @Timed(value = "order.get", description = "Time taken to get order")
    public ResponseEntity<Order> getOrder(@PathVariable String orderId) {
        log.info("FETching order; {}", orderId);

        return orderService.getOrder(orderId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
    
}
