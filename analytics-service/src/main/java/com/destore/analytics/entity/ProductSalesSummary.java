package com.destore.analytics.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "product_sales_summary")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSalesSummary {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 50)
    private String productCode;
    
    @Column(length = 200)
    private String productName;
    
    @Column(nullable = false)
    private LocalDate reportDate;
    
    @Column(nullable = false)
    private Integer quantitySold;
    
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalRevenue;
    
    @Column(precision = 12, scale = 2)
    private BigDecimal averagePrice;
    
    @Column(nullable = false)
    private Integer transactionCount;
    
    @Column(name = "last_updated")
    private java.time.LocalDateTime lastUpdated;
    
    @PreUpdate
    @PrePersist
    protected void onUpdate() {
        lastUpdated = java.time.LocalDateTime.now();
    }
}
