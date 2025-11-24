package com.destore.inventory.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 50)
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
