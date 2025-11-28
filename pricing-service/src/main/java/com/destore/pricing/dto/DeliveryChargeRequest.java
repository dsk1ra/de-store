package com.destore.pricing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request for calculating delivery charges.
 * Simplified delivery calculation as part of pricing service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryChargeRequest {
    
    private BigDecimal orderValue;
    private Double distanceKm;
    private boolean expressDelivery;
}
