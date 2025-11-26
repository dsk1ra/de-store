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
public class ProductSalesResponse {
    private String productCode;
    private String productName;
    private LocalDate reportDate;
    private Integer quantitySold;
    private BigDecimal totalRevenue;
    private BigDecimal averagePrice;
    private Integer transactionCount;
    private Integer rank;
}
