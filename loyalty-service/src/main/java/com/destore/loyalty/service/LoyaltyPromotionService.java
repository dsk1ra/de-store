package com.destore.loyalty.service;

import com.destore.loyalty.dto.LoyaltyPromotionRequest;
import com.destore.loyalty.entity.Customer;
import com.destore.loyalty.entity.LoyaltyPromotion;
import com.destore.loyalty.entity.LoyaltyTier;
import com.destore.loyalty.repository.CustomerRepository;
import com.destore.loyalty.repository.LoyaltyPromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoyaltyPromotionService {
    
    private final LoyaltyPromotionRepository promotionRepository;
    private final CustomerRepository customerRepository;
    
    @Transactional
    public LoyaltyPromotion createPromotion(LoyaltyPromotionRequest request) {
        if (promotionRepository.findByPromotionCode(request.getPromotionCode()).isPresent()) {
            throw new com.destore.exception.DuplicateResourceException("Loyalty Promotion", request.getPromotionCode());
        }
        
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new com.destore.exception.InvalidRequestException("Start date cannot be after end date");
        }
        
        LoyaltyPromotion promotion = LoyaltyPromotion.builder()
                .promotionCode(request.getPromotionCode())
                .promotionName(request.getPromotionName())
                .description(request.getDescription())
                .minTierRequired(request.getMinTierRequired())
                .discountPercentage(request.getDiscountPercentage())
                .pointsCost(request.getPointsCost())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .active(true)
                .build();
        
        LoyaltyPromotion saved = promotionRepository.save(promotion);
        log.info("Created loyalty promotion: {} for tier: {}", saved.getPromotionCode(), saved.getMinTierRequired());
        return saved;
    }
    
    public LoyaltyPromotion getPromotion(String promotionCode) {
        return promotionRepository.findByPromotionCode(promotionCode)
                .orElseThrow(() -> new com.destore.exception.ResourceNotFoundException("Loyalty Promotion", promotionCode));
    }
    
    public List<LoyaltyPromotion> getAllPromotions() {
        return promotionRepository.findAll();
    }
    
    public List<LoyaltyPromotion> getActivePromotions() {
        LocalDateTime now = LocalDateTime.now();
        return promotionRepository.findByActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(now, now);
    }
    
    public List<LoyaltyPromotion> getPromotionsForCustomer(String customerId) {
        Customer customer = customerRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new com.destore.exception.ResourceNotFoundException("Customer", customerId));
        
        LocalDateTime now = LocalDateTime.now();
        List<LoyaltyPromotion> activePromotions = promotionRepository
                .findByActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(now, now);
        
        // Filter promotions based on customer tier
        return activePromotions.stream()
                .filter(promo -> isTierEligible(customer.getLoyaltyTier(), promo.getMinTierRequired()))
                .filter(promo -> customer.getLoyaltyPoints() >= promo.getPointsCost())
                .collect(Collectors.toList());
    }
    
    @Transactional
    public void deactivatePromotion(String promotionCode) {
        LoyaltyPromotion promotion = getPromotion(promotionCode);
        promotion.setActive(false);
        promotionRepository.save(promotion);
        log.info("Deactivated loyalty promotion: {}", promotionCode);
    }
    
    private boolean isTierEligible(LoyaltyTier customerTier, LoyaltyTier requiredTier) {
        // GOLD can access all, SILVER can access SILVER and BRONZE, BRONZE only BRONZE
        return switch (customerTier) {
            case GOLD -> true;
            case SILVER -> requiredTier != LoyaltyTier.GOLD;
            case BRONZE -> requiredTier == LoyaltyTier.BRONZE;
        };
    }
}
