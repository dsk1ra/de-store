package com.destore.delivery.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_zones")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryZoneConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 50)
    @Enumerated(EnumType.STRING)
    private DeliveryZone zone;
    
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal multiplier;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal maxDistance;  // in kilometers
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal minDistance;  // in kilometers
    
    @Column(length = 500)
    private String description;
    
    @Column(nullable = false)
    private Boolean isActive;
    
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
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
