package com.destore.delivery.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_pricing")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryPricing {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal baseCharge;
    
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal ratePerKm;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal freeDistanceThreshold;  // km
    
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal freeDeliveryThreshold;  // order value
    
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal reducedRateThreshold;  // order value
    
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal reducedRatePercentage;
    
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal expressSurcharge;
    
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal peakHourSurchargePercentage;
    
    @Column(nullable = false)
    private Boolean isActive;
    
    @Column(name = "effective_from")
    private LocalDateTime effectiveFrom;
    
    @Column(name = "effective_until")
    private LocalDateTime effectiveUntil;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
        if (effectiveFrom == null) {
            effectiveFrom = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
