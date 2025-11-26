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
@Table(name = "store_performance")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorePerformance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 50)
    private String storeId;
    
    @Column(nullable = false)
    private LocalDate reportDate;
    
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalSales;
    
    @Column(nullable = false)
    private Integer transactionCount;
    
    @Column(nullable = true)
    private Integer customerCount;
    
    @Column(precision = 12, scale = 2)
    private BigDecimal averageTransactionValue;
    
    @Column(precision = 12, scale = 2)
    private BigDecimal totalDiscounts;
    
    @Column(precision = 12, scale = 2)
    private BigDecimal netRevenue;
    
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
    
    @PreUpdate
    @PrePersist
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }
}
