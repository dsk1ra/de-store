package com.destore.pricing.dto;

import com.destore.pricing.entity.Promotion;
import com.destore.pricing.entity.PromotionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionResponse {
    private Long id;
    private String promotionCode;
    private String promotionName;
    private PromotionType promotionType;
    private BigDecimal discountValue;
    private List<String> applicableProducts;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean active;
    private LocalDateTime createdAt;

    public static PromotionResponse fromEntity(Promotion promotion) {
        // Convert comma-separated string to list
        List<String> productsList = Collections.emptyList();
        if (promotion.getApplicableProducts() != null && !promotion.getApplicableProducts().isEmpty()) {
            productsList = Arrays.asList(promotion.getApplicableProducts().split(","));
        }

        return PromotionResponse.builder()
                .id(promotion.getId())
                .promotionCode(promotion.getPromotionCode())
                .promotionName(promotion.getPromotionName())
                .promotionType(promotion.getPromotionType())
                .discountValue(promotion.getDiscountValue())
                .applicableProducts(productsList)
                .startDate(promotion.getStartDate())
                .endDate(promotion.getEndDate())
                .active(promotion.getActive())
                .createdAt(promotion.getCreatedAt())
                .build();
    }
}
