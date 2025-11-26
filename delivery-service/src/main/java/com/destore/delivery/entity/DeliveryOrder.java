package com.destore.delivery.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryOrder {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 100)
    private String orderId;
    
    @Column(nullable = false, length = 100)
    private String customerId;
    
    @Column(length = 200)
    private String customerName;
    
    @Column(nullable = false, length = 100)
    private String storeId;
    
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal orderValue;
    
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal deliveryCharge;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal distance;  // in kilometers
    
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private DeliveryZone zone;
    
    @Column(nullable = false)
    private Boolean isExpress;
    
    @Column(nullable = false)
    private Boolean isPeakHour;
    
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;
    
    @Column(nullable = false, length = 500)
    private String deliveryAddress;
    
    @Column(length = 500)
    private String pickupAddress;
    
    @Column(nullable = false)
    private LocalDateTime orderDateTime;
    
    @Column
    private LocalDateTime estimatedDeliveryTime;
    
    @Column
    private LocalDateTime actualDeliveryTime;
    
    @Column(length = 200)
    private String driverName;
    
    @Column(length = 50)
    private String driverPhone;
    
    @Column(length = 100)
    private String vehicleNumber;
    
    @Column(length = 1000)
    private String notes;
    
    @Column(precision = 12, scale = 2)
    private BigDecimal baseCharge;
    
    @Column(precision = 12, scale = 2)
    private BigDecimal distanceCharge;
    
    @Column(precision = 12, scale = 2)
    private BigDecimal zoneCharge;
    
    @Column(precision = 12, scale = 2)
    private BigDecimal expressCharge;
    
    @Column(precision = 12, scale = 2)
    private BigDecimal peakHourCharge;
    
    @Column(precision = 12, scale = 2)
    private BigDecimal discount;
    
    @Column(name = "created_at")
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
