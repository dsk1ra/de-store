package com.destore.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceMetrics {
    private String storeId;
    private String period;
    private BigDecimal totalRevenue;
    private Integer totalTransactions;
    private Integer uniqueCustomers;
    private BigDecimal averageOrderValue;
    private BigDecimal growthRate;
    private String status;
}
