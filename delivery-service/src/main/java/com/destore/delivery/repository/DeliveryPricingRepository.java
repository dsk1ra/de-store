package com.destore.delivery.repository;

import com.destore.delivery.entity.DeliveryPricing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface DeliveryPricingRepository extends JpaRepository<DeliveryPricing, Long> {
    
    @Query("SELECT d FROM DeliveryPricing d WHERE d.isActive = true " +
           "AND d.effectiveFrom <= CURRENT_TIMESTAMP " +
           "AND (d.effectiveUntil IS NULL OR d.effectiveUntil >= CURRENT_TIMESTAMP) " +
           "ORDER BY d.effectiveFrom DESC")
    Optional<DeliveryPricing> findActivePrice();
    
    Optional<DeliveryPricing> findFirstByIsActiveTrueOrderByEffectiveFromDesc();
}
