package com.destore.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAnalyticsResponse {
    private String customerId;
    private String customerName;
    private Integer totalPurchases;
    private BigDecimal totalSpent;
    private BigDecimal averageOrderValue;
    private LocalDate firstPurchaseDate;
    private LocalDate lastPurchaseDate;
    private Integer daysSinceLastPurchase;
    private String loyaltyTier;
    private BigDecimal projectedLifetimeValue;
}
