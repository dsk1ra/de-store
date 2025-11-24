package com.destore.pricing.dto;

import com.destore.pricing.entity.PromotionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionRequest {
    private String promotionCode;
    private String promotionName;
    private PromotionType promotionType;
    private BigDecimal discountValue;
    private List<String> applicableProducts;
    private LocalDate startDate;
    private LocalDate endDate;
}
