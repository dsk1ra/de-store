package com.destore.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRequest {
    private String customerId;
    private String customerName;
    private String storeId;
    private List<PurchaseItem> items;
    private BigDecimal totalAmount;
    
    // Delivery options
    private Boolean requiresDelivery;
    private BigDecimal deliveryDistance;
    private String deliveryAddress;
    private Boolean isExpressDelivery;
    
    // Payment
    private String paymentMethod;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PurchaseItem {
        private String productCode;
        private Integer quantity;
    }
}

