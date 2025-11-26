package com.destore.loyalty.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 50)
    private String customerId;
    
    @Column(nullable = false, length = 50)
    private String orderId;
    
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
    
    @Column(nullable = false)
    private Integer pointsEarned;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoyaltyTier tierAtPurchase;
    
    @Column(columnDefinition = "TEXT")
    private String items;
    
    @Column(name = "purchase_date", nullable = false, updatable = false)
    private LocalDateTime purchaseDate;
    
    @PrePersist
    protected void onCreate() {
        purchaseDate = LocalDateTime.now();
    }
}
