package com.destore.inventory.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {
    
    @Id
    @Column(length = 100)
    private String reservationId;
    
    @Column(nullable = false, length = 50)
    private String productCode;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(length = 500)
    private String initialNotes;  // Initial notes provided during reservation
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    
    @Column
    private LocalDateTime processedAt;
    
    @Column(length = 500)
    private String notes;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = ReservationStatus.PENDING;
        }
    }
}
