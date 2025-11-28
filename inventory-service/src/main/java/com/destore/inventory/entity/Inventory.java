package com.destore.inventory.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory", 
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_inventory_product_store", columnNames = {"productCode", "storeId"})
    },
    indexes = {
        @Index(name = "idx_inventory_product_code", columnList = "productCode"),
        @Index(name = "idx_inventory_store_id", columnList = "storeId"),
        @Index(name = "idx_inventory_product_store", columnList = "productCode, storeId"),
        @Index(name = "idx_inventory_low_stock", columnList = "quantity, reservedQuantity, lowStockThreshold")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 50)
    private String productCode;
    
    @Builder.Default
    @Column(nullable = false)
    private Integer quantity = 0;
    
    @Builder.Default
    @Column(nullable = false)
    private Integer reservedQuantity = 0;
    
    @Builder.Default
    @Column(nullable = false)
    private Integer lowStockThreshold = 10;
    
    @Column(nullable = false, length = 50)
    private String storeId;
    
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
    
    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }
    
    public Integer getAvailableQuantity() {
        return quantity - reservedQuantity;
    }
}
