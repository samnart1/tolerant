package com.tolerant.inventory_service.model;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InventoryResponse {
    private String reservationId;
    private boolean available;
    private String message;
    private Long processingTimeMs;
    private Integer quantityAvailable;
    private Integer quantityRequested;
}
