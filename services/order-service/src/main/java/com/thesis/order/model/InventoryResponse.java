package com.thesis.order.model;

import lombok.Data;

@Data
public class InventoryResponse {
    private String productId;
    private Integer availableQuantity;
    private boolean available;
}
