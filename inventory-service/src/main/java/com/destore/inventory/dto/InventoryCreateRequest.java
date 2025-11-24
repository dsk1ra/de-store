package com.destore.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryCreateRequest {
    private String productCode;
    private Integer quantity;
    private Integer lowStockThreshold;
    private String storeId;
}
