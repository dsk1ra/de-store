package com.destore.pricing.dto;

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
public class PriceCalculationResponse {
    private BigDecimal subtotal;
    private BigDecimal promotionalDiscount;
    private BigDecimal finalTotal;
    private List<String> appliedPromotions;
    private List<ItemBreakdown> itemBreakdown;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemBreakdown {
        private String productCode;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
        private BigDecimal discount;
        private BigDecimal total;
    }
}
