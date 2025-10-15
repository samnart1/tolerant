package com.tolerant.inventory_service.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import com.tolerant.inventory_service.model.Inventory;

import jakarta.persistence.LockModeType;


@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Inventory> findByProductId(String productId);

    boolean existsByProductId(String productId);    
}
