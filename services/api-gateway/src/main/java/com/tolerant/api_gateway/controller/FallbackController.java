package com.tolerant.api_gateway.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/fallback")
@Slf4j
public class FallbackController {
    
    @RequestMapping("/orders")
    public ResponseEntity<Map<String, Object>> orderServiceFallback() {
        log.warn("Order Service fallback triggered");
        return buildFallbackResponse("Order Service is currently unavailable. Please try again late.");
    }

    @RequestMapping("/inventory")
    public ResponseEntity<Map<String, Object>> inventoryServiceFallback() {
        log.warn("Inventory Service fallback triggered");
        return buildFallbackResponse("Inventory Service is currently unavailable. Pleae try again later.");
    }

    @RequestMapping("/payments")
    public ResponseEntity<Map<String, Object>> paymentServiceFallback() {
        log.warn("Payment Service fallback triggered");
        return buildFallbackResponse("Payment Service is currently unavailable. Please try again later.");
    }

    @RequestMapping("/notifications")
    public ResponseEntity<Map<String, Object>> notificationServiceFallback() {
        log.warn("Notification Service fallback triggered");
        return buildFallbackResponse("Notification Service is currently unavailable. Please try again later.");
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return new ResponseEntity<>("API Gateway is healthy", HttpStatus.OK);
    }

    private ResponseEntity<Map<String, Object>> buildFallbackResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("error", "Service Unavailable");
        response.put("message", message);
        response.put("fallback", true);

        return new ResponseEntity<>(response, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
