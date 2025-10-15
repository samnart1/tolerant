package com.tolerant.inventory_service.service;

import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tolerant.inventory_service.model.Inventory;
import com.tolerant.inventory_service.model.InventoryRequest;
import com.tolerant.inventory_service.model.InventoryResponse;
import com.tolerant.inventory_service.model.Reservation;
import com.tolerant.inventory_service.repo.InventoryRepository;
import com.tolerant.inventory_service.repo.ReservationRepository;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryManagementService implements InventoryService {
    
    private final InventoryRepository inventoryRepo;
    private final ReservationRepository reservationRepo;
    private final Random random = new Random();

    @Value("${chaos.enabled:false}")
    private boolean chaosEnabled;

    @Value("${chaos.failure-rate:0.0}")
    private double chaosFailureRate;

    @Value("${chaos.delay-ms:0}")
    private long chaosDelayMs;

    @Value("${inventory.processing.base-delay-ms:50}")
    private long baseProcessingDelayMs;

    @Value("${inventory.processing.random-delay-ms:100}")
    private long randomProcessingDelayMs;

    @Override
    @Timed(value = "inventory.check", description = "Time taken to check inventory")
    public InventoryResponse checkAvailability(InventoryRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("checking inventory for product: {}, quantity: {}", request.getProductId(), request.getQuantity());

        try {
            if (chaosEnabled) {
                applyChaos();
            }

            simulateProcessingDelay();

            Inventory inventory = inventoryRepo.findByProductId(request.getProductId())
                .orElseThrow(() -> new InventoryException("Product not found: {}", request.getProductId()));

            boolean available = inventory.canReserve(request.getQuantity());

            long processingTime = System.currentTimeMillis() - startTime;

            log.info("Inventory check complete. Product: {}, Available: {}, Quantity: {}/{}", request.getProductId(), available, inventory.getQuantityAvailable(), request.getQuantity());

            return InventoryResponse.builder()
                .available(available)
                .message(available ? "Inventory available" : "Insufficient stock")
                .processingTimeMs(processingTime)
                .quantityAvailable(inventory.getQuantityAvailable())
                .quantityRequested(request.getQuantity())
                .build();

        } catch (InventoryException e) {
            throw e;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InventoryException("Inventory check interrupted");
        }
    }

    private void applyChaos() {}

    private void simulateProcessingDelay() {}

    @Transactional
    @Override
    @Timed(value = "inventory.reserve", description = "Time taken to reserve inventory")
    public InventoryResponse reserveInventory(InventoryRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Reserving inventory for order: {}, product: {}, quantity: {}", request.getOrderId(), request.getProductId(), request.getQuantity());

        
    }

    @Override
    public void releaseReservation(String reservationId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'releaseReservation'");
    }

    @Override
    public Inventory getInventoryByProductId(String productId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getInventoryByProductId'");
    }

    @Override
    public List<Inventory> getAllInventory() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAllInventory'");
    }

    @Override
    public Reservation getReservation(String reservationId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getReservation'");
    }
}
