package com.tolerant.inventory_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tolerant.inventory_service.model.InventoryRequest;
import com.tolerant.inventory_service.model.InventoryResponse;
import com.tolerant.inventory_service.model.Reservation;
import com.tolerant.inventory_service.service.InventoryService;

import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/inventory")
@Slf4j
@RequiredArgsConstructor
public class InventoryController {
    
    private final InventoryService inventoryService;

    @PostMapping("/check")
    @Timed(value = "inventory.check.api", description = "Time taken for inventory check API")
    public ResponseEntity<InventoryResponse> checkAvailability(@Valid @RequestBody InventoryRequest request) {
        InventoryResponse response = inventoryService.checkAvailability(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/reserve")
    @Timed(value = "inventory.reserve.api", description = "Time taken for inventory reservation API")
    public ResponseEntity<InventoryResponse> reserveInventory(@Valid @RequestBody InventoryRequest request) {
        log.info("Received inventory reservation request for order: {}", request.getOrderId());
        InventoryResponse response = inventoryService.reserveInventory(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @DeleteMapping("/release/{reservationId}")
    @Timed(value = "inventory.getReservation", description = "Time taken to get reservation")
    public ResponseEntity<Reservation> getReservation(@PathVariable String reservationId) {
        Reservation reservation = inventoryService.getReservation(reservationId);
        return new ResponseEntity<>(reservation, HttpStatus.OK);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return new ResponseEntity<>("Inventory Service is healthy", HttpStatus.OK);
    }
}
