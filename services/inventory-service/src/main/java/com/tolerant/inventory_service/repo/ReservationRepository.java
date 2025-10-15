package com.tolerant.inventory_service.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tolerant.inventory_service.model.Reservation;
import com.tolerant.inventory_service.model.ReservationStatus;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    Optional<Reservation> findByReservationId(String reservationId);
    Optional<Reservation> findByOrderId(String orderId);
    List<Reservation> findByProductId(String productId);
    List<Reservation> findByStatus(ReservationStatus status);
    long countByStatus(ReservationStatus status);
}
