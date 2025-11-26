package com.destore.loyalty.dto;

import com.destore.loyalty.entity.LoyaltyTier;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyPromotionRequest {
    
    @NotBlank(message = "Promotion code is required")
    private String promotionCode;
    
    @NotBlank(message = "Promotion name is required")
    private String promotionName;
    
    private String description;
    
    @NotNull(message = "Minimum tier is required")
    private LoyaltyTier minTierRequired;
    
    @NotNull(message = "Discount percentage is required")
    private Integer discountPercentage;
    
    @NotNull(message = "Points cost is required")
    private Integer pointsCost;
    
    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;
    
    @NotNull(message = "End date is required")
    private LocalDateTime endDate;
}
