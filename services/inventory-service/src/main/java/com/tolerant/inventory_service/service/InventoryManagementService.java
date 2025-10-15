package com.tolerant.inventory_service.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tolerant.inventory_service.exception.InsufficientStockException;
import com.tolerant.inventory_service.exception.InventoryException;
import com.tolerant.inventory_service.model.Inventory;
import com.tolerant.inventory_service.model.InventoryRequest;
import com.tolerant.inventory_service.model.InventoryResponse;
import com.tolerant.inventory_service.model.Reservation;
import com.tolerant.inventory_service.model.ReservationStatus;
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
                .orElseThrow(() -> new InventoryException("Product not found: " + request.getProductId()));

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

    private void applyChaos() {
        if (chaosDelayMs > 0) {
            try {
                log.debug("Applying chaos delay: {}", chaosDelayMs);
                Thread.sleep(chaosDelayMs);

            } catch(InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (chaosFailureRate > 0 && random.nextDouble() < chaosFailureRate) {
            log.warn("Chaos: Injecting inventory failure");
            throw new InventoryException("Chaos engineering: Random inventory failure");
        }
    }

    private void simulateProcessingDelay() throws InterruptedException {
        long delay = baseProcessingDelayMs + random.nextInt((int) randomProcessingDelayMs);
        log.debug("Simulating inventory processing delay: {}ms", delay);
        Thread.sleep(delay);
    }

    @Transactional
    @Override
    @Timed(value = "inventory.reserve", description = "Time taken to reserve inventory")
    public InventoryResponse reserveInventory(InventoryRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Reserving inventory for order: {}, product: {}, quantity: {}", request.getOrderId(), request.getProductId(), request.getQuantity());

        Reservation reservation = Reservation.builder()
            .orderId(request.getOrderId())
            .productId(request.getProductId())
            .quantity(request.getQuantity())
            .status(ReservationStatus.PENDING)
            .build();        

        reservation = reservationRepo.save(reservation);

        try {
            if (chaosEnabled) {
                applyChaos();
            }

            simulateProcessingDelay();

            Inventory inventory = inventoryRepo.findByProductId(request.getProductId())
                .orElseThrow(() -> new InventoryException("Product not found: " + request.getProductId()));

            if (!inventory.canReserve(request.getQuantity())) {
                reservation.setStatus(ReservationStatus.FAILED);
                reservation.setProcessingTimeMs(System.currentTimeMillis() - startTime);
                reservationRepo.save(reservation);

                throw new InsufficientStockException(String.format("Insufficient stock for product %s. Available: %d, Requested: %d", request.getProductId(), inventory.getQuantityAvailable(), request.getQuantity()));
            }

            inventory.setQuantityAvailable(inventory.getQuantityAvailable() - request.getQuantity());
            inventory.setQuantityReserved(inventory.getQuantityReserved() + request.getQuantity());
            inventoryRepo.save(inventory);

            reservation.setStatus(ReservationStatus.RESERVED);
            reservation.setCompletedAt(LocalDateTime.now());
            reservation.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            reservation = reservationRepo.save(reservation);

            log.info("Inventory reserved successfully. Reservation: {}, Product: {}, Quantity: {}", reservation.getReservationId(), request.getProductId(), request.getQuantity());

            return InventoryResponse.builder()
                .reservationId(reservation.getReservationId())
                .available(true)
                .message("Inventory reserved successfully")
                .processingTimeMs(reservation.getProcessingTimeMs())
                .quantityAvailable(inventory.getQuantityAvailable())
                .quantityRequested(request.getQuantity())
                .build();
                
        } catch (InventoryException | InsufficientStockException e) {
            log.error("Inventory reservation failed: {}", e.getMessage());
            reservation.setStatus(ReservationStatus.FAILED);
            reservation.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            reservationRepo.save(reservation);
            throw e;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            reservation.setStatus(ReservationStatus.FAILED);
            reservation.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            reservationRepo.save(reservation);
            throw new InventoryException("Inventory reservation interrupted");
        }
    }

    @Override
    public void releaseReservation(String reservationId) {
        log.info("Releasing reservation: {}", reservationId);

        Reservation reservation = reservationRepo.findByReservationId(reservationId)
            .orElseThrow(() -> new InventoryException("Reservation not found: " + reservationId));

        if (reservation.getStatus() != ReservationStatus.RESERVED) {
            throw new InventoryException("Reservation cannot be released. Status: " + reservation.getStatus());
        }

        String productId = reservation.getProductId();
        Inventory inventory = inventoryRepo.findByProductId(productId)
            .orElseThrow(() -> new InventoryException("Inventory not found: " + productId));

        inventory.setQuantityAvailable(inventory.getQuantityAvailable() + reservation.getQuantity());
        inventory.setQuantityReserved(inventory.getQuantityReserved() - reservation.getQuantity());
        inventoryRepo.save(inventory);

        reservation.setStatus(ReservationStatus.CANCELED);
        reservationRepo.save(reservation);

        log.info("Reservation released successfully: {}", reservationId);
    }

    @Override
    public Inventory getInventoryByProductId(String productId) {
        return inventoryRepo.findByProductId(productId)
            .orElseThrow(() -> new InventoryException("Inventory not found: " + productId));
    }

    @Override
    public List<Inventory> getAllInventory() {
        return inventoryRepo.findAll();
    }

    @Override
    public Reservation getReservation(String reservationId) {
        return reservationRepo.findByReservationId(reservationId)
            .orElseThrow(() -> new InventoryException("Reservation not found: " + reservationId));
    }

    public void updateChaosConfig(double failureRate, long delayMs) {
        this.chaosFailureRate = Math.min(Math.max(failureRate, 0.0), 1.0);
        this.chaosDelayMs = Math.max(delayMs, 0);
        log.info("Chaos config updated. Failure Rate: {}%, Delay: {}ms", this.chaosFailureRate, this.chaosDelayMs);
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ChaosConfig {
        private boolean enabled;
        private double failureRate;
        private long delayMs;
    }

    public ChaosConfig getChaosConfig() {
        return new ChaosConfig(chaosEnabled, chaosFailureRate, baseProcessingDelayMs);
    }
}
