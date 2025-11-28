package com.destore.pricing.repository;

import com.destore.pricing.entity.Promotion;
import com.destore.pricing.entity.PromotionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    Optional<Promotion> findByPromotionCode(String promotionCode);
    List<Promotion> findByActiveTrue();
    List<Promotion> findByActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
        LocalDate start, LocalDate end);
    List<Promotion> findByPromotionTypeAndActive(PromotionType promotionType, Boolean active);
}
