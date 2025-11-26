package com.destore.loyalty.repository;

import com.destore.loyalty.entity.LoyaltyPromotion;
import com.destore.loyalty.entity.LoyaltyTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoyaltyPromotionRepository extends JpaRepository<LoyaltyPromotion, Long> {
    Optional<LoyaltyPromotion> findByPromotionCode(String promotionCode);
    List<LoyaltyPromotion> findByActiveTrue();
    List<LoyaltyPromotion> findByMinTierRequired(LoyaltyTier tier);
    List<LoyaltyPromotion> findByActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            LocalDateTime startDate, LocalDateTime endDate);
}
