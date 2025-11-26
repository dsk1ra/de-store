package com.destore.loyalty.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 50)
    private String customerId;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(nullable = false, unique = true, length = 100)
    private String email;
    
    @Column(length = 20)
    private String phone;
    
    @Column(length = 200)
    private String address;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer loyaltyPoints = 0;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private LoyaltyTier loyaltyTier = LoyaltyTier.BRONZE;
    
    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalSpent = BigDecimal.ZERO;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer purchaseCount = 0;
    
    @Column(name = "joined_date", nullable = false, updatable = false)
    private LocalDateTime joinedDate;
    
    @Column(name = "last_purchase_date")
    private LocalDateTime lastPurchaseDate;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
    
    @PrePersist
    protected void onCreate() {
        joinedDate = LocalDateTime.now();
    }
    
    // Helper method to check if customer is a regular customer
    public boolean isRegularCustomer() {
        return purchaseCount >= 3 && active;
    }
    
    // Helper method to calculate discount percentage based on tier
    public int getDiscountPercentage() {
        return switch (loyaltyTier) {
            case BRONZE -> 0;
            case SILVER -> 5;
            case GOLD -> 10;
        };
    }
}
