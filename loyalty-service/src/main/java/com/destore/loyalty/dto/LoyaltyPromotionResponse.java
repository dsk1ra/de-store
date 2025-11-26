package com.destore.loyalty.dto;

import com.destore.loyalty.entity.LoyaltyPromotion;
import com.destore.loyalty.entity.LoyaltyTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyPromotionResponse {
    private Long id;
    private String promotionCode;
    private String promotionName;
    private String description;
    private LoyaltyTier minTierRequired;
    private Integer discountPercentage;
    private Integer pointsCost;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean active;
    private LocalDateTime createdAt;
    
    public static LoyaltyPromotionResponse fromEntity(LoyaltyPromotion promotion) {
        return LoyaltyPromotionResponse.builder()
                .id(promotion.getId())
                .promotionCode(promotion.getPromotionCode())
                .promotionName(promotion.getPromotionName())
                .description(promotion.getDescription())
                .minTierRequired(promotion.getMinTierRequired())
                .discountPercentage(promotion.getDiscountPercentage())
                .pointsCost(promotion.getPointsCost())
                .startDate(promotion.getStartDate())
                .endDate(promotion.getEndDate())
                .active(promotion.getActive())
                .createdAt(promotion.getCreatedAt())
                .build();
    }
}
