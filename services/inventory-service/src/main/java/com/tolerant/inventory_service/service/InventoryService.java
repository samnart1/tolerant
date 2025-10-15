package com.tolerant.inventory_service.service;

import java.util.List;

import com.tolerant.inventory_service.model.Inventory;
import com.tolerant.inventory_service.model.InventoryRequest;
import com.tolerant.inventory_service.model.InventoryResponse;
import com.tolerant.inventory_service.model.Reservation;

public interface InventoryService {
    InventoryResponse checkAvailability(InventoryRequest request);
    InventoryResponse reserveInventory(InventoryRequest request);
    void releaseReservation(String reservationId);
    Inventory getInventoryByProductId(String productId);
    List<Inventory> getAllInventory();
    Reservation getReservation(String reservationId);
}
