package com.tolerant.order_service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tolerant.order_service.model.Order;
import com.tolerant.order_service.model.OrderRequest;
import com.tolerant.order_service.service.OrderService;

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
    @Timed(value = "order.create", description = "Time taken to create an order")
    public ResponseEntity<Order> createOrder(@Valid @RequestBody OrderRequest request) {
        log.info("Received order request for customer: {}", request.getCustomerId());

        Order order = orderService.createOrder(request);

        return new ResponseEntity<>(order, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Timed(value = "order.get", description = "Time taken to get an order")
    public ResponseEntity<Order> getOrder(@PathVariable Long id) {
        Order order = orderService.getOrder(id);

        return new ResponseEntity<>(order, HttpStatus.OK);
    }

    @GetMapping("/number/{orderNumber}")
    @Timed(value = "order.getByNumber", description = "Time taken to get order by number")
    public ResponseEntity<Order> getOrderByNumber(@PathVariable String orderNumber) {
        Order order = orderService.getOrderByNumber(orderNumber);

        return new ResponseEntity<>(order, HttpStatus.OK);
    }

    @GetMapping
    @Timed(value = "order.getAll", description = "Time taken to get all orders")
    public ResponseEntity<List<Order>> getAllOrders() {
        List<Order> orders = orderService.getAllOrders();
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @GetMapping("/customer/{customerId}")
    @Timed(value = "order.getByCustomer", description = "Time taken to get orders by customer")
    public ResponseEntity<List<Order>> getOrdersByCustomer(@PathVariable String customerId) {
        List<Order> orders = orderService.getOrdersByCustomer(customerId);

        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return new ResponseEntity<>("Order Service is healthy", HttpStatus.OK);
    }
}
