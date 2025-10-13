package com.tolerant.order_service.client;

import java.time.Duration;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.tolerant.order_service.model.InventoryRequest;
import com.tolerant.order_service.model.InventoryResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryClient {

    private final WebClient.Builder webClientBuilder;

    public Mono<InventoryResponse> reserveInventory(InventoryRequest inventoryRequest, Duration timeout) {
        log.info("Calling inventory service for order: {}", inventoryRequest.getOrderId());

        return webClientBuilder.build()
            .post()
            .uri("http://inventory-service/api/inventory/reserve")
            .bodyValue(inventoryRequest)
            .retrieve()
            .bodyToMono(InventoryResponse.class)
            .timeout(timeout)
            .doOnSuccess(response -> log.info("Inventory reserved: {}", response.getReservationId()))
            .doOnError(error -> log.info("Inventory reservation failed: {}", error.getMessage()));
    }

    public InventoryResponse reserveInventorySync(InventoryRequest inventoryRequest, Duration timeout) {
        return reserveInventory(inventoryRequest, timeout).block();
    }

}
