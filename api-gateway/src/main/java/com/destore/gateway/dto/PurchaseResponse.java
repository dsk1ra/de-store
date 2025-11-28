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
public class PurchaseResponse {
    private boolean success;
    private String message;
    private String orderId;
    private String financeRequestId;
    
    // Pricing details
    private BigDecimal subtotal;
    private BigDecimal promotionalDiscount;
    private BigDecimal loyaltyDiscount;
    private BigDecimal deliveryCharge;
    private BigDecimal finalTotal;
    private List<String> appliedPromotions;
    
    // Loyalty details
    private Integer loyaltyPointsEarned;
    private Integer newLoyaltyPointsBalance;
    private String loyaltyTier;
    
    // Delivery details
    private String deliveryOrderId;
    private String estimatedDeliveryTime;
}

