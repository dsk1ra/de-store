package com.destore.analytics.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_analytics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAnalytics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 50)
    private String customerId;
    
    @Column(length = 100)
    private String customerName;
    
    @Column(nullable = false)
    private LocalDate reportDate;
    
    @Column(nullable = false)
    private Integer totalPurchases;
    
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalSpent;
    
    @Column(precision = 12, scale = 2)
    private BigDecimal averageOrderValue;
    
    @Column
    private LocalDate lastPurchaseDate;
    
    @Column
    private LocalDate firstPurchaseDate;
    
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
    
    @PreUpdate
    @PrePersist
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }
}
