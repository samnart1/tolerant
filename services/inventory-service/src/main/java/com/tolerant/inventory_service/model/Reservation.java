package com.tolerant.inventory_service.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "reservations")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String reservationId;

    @Column(nullable = false)
    private String orderId;

    @Column(nullable = false)
    private String productId;

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // @Column(nullable = false)
    private LocalDateTime completedAt;

    // @Column(nullable = false)
    private Long processingTimeMs;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (reservationId == null) {
            reservationId = "RES-" + System.currentTimeMillis();
        }
    }
}
