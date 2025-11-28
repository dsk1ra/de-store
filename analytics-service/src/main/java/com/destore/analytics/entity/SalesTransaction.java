package com.destore.analytics.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sales_transactions", indexes = {
    @Index(name = "idx_sales_order_id", columnList = "orderId"),
    @Index(name = "idx_sales_customer_id", columnList = "customerId"),
    @Index(name = "idx_sales_store_id", columnList = "storeId"),
    @Index(name = "idx_sales_transaction_date", columnList = "transactionDate"),
    @Index(name = "idx_sales_customer_date", columnList = "customerId, transactionDate"),
    @Index(name = "idx_sales_store_date", columnList = "storeId, transactionDate")
})
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
    
    @Column(nullable = false, unique = true, length = 50)
    private String orderId;
    
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
