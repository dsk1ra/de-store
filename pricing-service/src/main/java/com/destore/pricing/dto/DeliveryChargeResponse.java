package com.destore.pricing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response for delivery charge calculation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryChargeResponse {
    
    private BigDecimal baseCharge;
    private BigDecimal distanceCharge;
    private BigDecimal expressCharge;
    private BigDecimal totalCharge;
    private boolean freeDelivery;
    private String freeDeliveryReason;
}
