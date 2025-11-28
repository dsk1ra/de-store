package com.destore.inventory.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Store entity representing a physical or virtual store location.
 * All inventory must be associated with a valid store.
 */
@Entity
@Table(name = "stores", indexes = {
    @Index(name = "idx_store_id", columnList = "storeId"),
    @Index(name = "idx_store_name", columnList = "name"),
    @Index(name = "idx_store_active", columnList = "active")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Store {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 50)
    private String storeId;
    
    @Column(nullable = false, length = 200)
    private String name;
    
    @Column(length = 500)
    private String address;
    
    @Column(length = 100)
    private String city;
    
    @Column(length = 100)
    private String region;
    
    @Column(length = 20)
    private String postalCode;
    
    @Column(length = 100)
    private String country;
    
    @Column(length = 50)
    private String phone;
    
    @Column(length = 100)
    private String email;
    
    @Column(length = 100)
    private String managerName;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
