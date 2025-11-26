package com.destore.analytics.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sales_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 50)
    private String transactionId;
    
    @Column(nullable = false, length = 50)
    private String customerId;
    
    @Column(length = 100)
    private String customerName;
    
    @Column(nullable = false, length = 50)
    private String storeId;
    
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;
    
    @Column(precision = 12, scale = 2)
    private BigDecimal discountAmount;
    
    @Column(precision = 12, scale = 2)
    private BigDecimal taxAmount;
    
    @Column(precision = 12, scale = 2)
    private BigDecimal netAmount;
    
    @Column(length = 50)
    private String paymentMethod;
    
    @Column(length = 20)
    private String transactionStatus;
    
    @Column(columnDefinition = "TEXT")
    private String items;
    
    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (transactionDate == null) {
            transactionDate = LocalDateTime.now();
        }
    }
}
