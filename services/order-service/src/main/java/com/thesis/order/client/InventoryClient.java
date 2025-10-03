package com.thesis.order.client;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.thesis.order.model.InventoryResponse;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryClient {
    
    private final RestTemplate restTemplate;

    @Value("${inventory.service.url:http://inventory-service:8081}")
    private String inventoryServiceURL;

    @CircuitBreaker(name = "inventoryClient", fallbackMethod = "checkInventoryFallback")
    @Retry(name = "inventoryClient")
    public InventoryResponse checkInventory(String productId, Integer quantity) {
        String url = String.format("%s/api/inventory/check?productId=%s&quantity=%d", inventoryServiceURL, productId, quantity);

        log.info("Checking inventory at: {}", url);

        try {
            ResponseEntity<InventoryResponse> response = restTemplate.getForEntity(url, InventoryResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("Inventory check successful: available={}", response.getBody().isAvailable());
                return response.getBody();
            }

            throw new RuntimeException("Invalid response from inventory service");

        } catch(Exception e) {
            log.error("Inventory check failed: {}", e.getMessage());
            throw new RuntimeException("failed to check inventory: " + e.getMessage(), e);
        }
    }


    @CircuitBreaker(name = "inventoryClient", fallbackMethod = "reserveInventoryFallback")
    @Retry(name = "inventoryClient")
    public void reserveInventory(String productId, Integer quantity) {

        String url = String.format("%s/api/inventory/reserve", inventoryServiceURL);

        Map<String, Object> request = new HashMap<>();
        request.put("productId", productId);
        request.put("quantity", quantity);

        log.info("Reserving inventory: product={}, quantity={}", productId, quantity);

        try {
            restTemplate.postForEntity(url, request, Void.class);
            log.info("Inventory reserved successfully");

        } catch (Exception e) {
            log.error("Failed to reserve inventory: {}", e.getMessage());
            throw new RuntimeException("Failed to reserve inventory: " + e.getMessage(), e);
        }
    }

    private InventoryResponse checkInventoryFallback(String productId, Integer quantity, Exception e) {
        log.warn("Inventory check fallback triggered: {}", e.getMessage());

        InventoryResponse response = new InventoryResponse();
        response.setProductId(productId);
        response.setAvailableQuantity(0);
        response.setAvailable(false);

        return response;
    }

    private void reserveInventoryFallback(String productId, Integer quantity, Exception e) {
        log.warn("Inventory reservation fallback triggered: {}", e.getMessage());
        throw new RuntimeException("Inventory service unavailable", e);
    }

}