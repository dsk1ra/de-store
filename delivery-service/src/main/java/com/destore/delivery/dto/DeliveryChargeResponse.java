package com.destore.delivery.dto;

import com.destore.delivery.entity.DeliveryZone;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryChargeResponse {
    
    private String orderId;
    private BigDecimal orderValue;
    private BigDecimal distance;
    private DeliveryZone zone;
    private Boolean isExpress;
    private Boolean isPeakHour;
    
    // Charge breakdown
    private BigDecimal baseCharge;
    private BigDecimal distanceCharge;
    private BigDecimal zoneCharge;
    private BigDecimal expressCharge;
    private BigDecimal peakHourCharge;
    private BigDecimal discount;
    private BigDecimal totalDeliveryCharge;
    
    // Calculation details
    private String discountReason;
    private String chargeBreakdown;
    
    // Order details
    private BigDecimal grandTotal;  // orderValue + deliveryCharge
}
