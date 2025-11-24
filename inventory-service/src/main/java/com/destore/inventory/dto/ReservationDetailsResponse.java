package com.destore.inventory.dto;

import com.destore.inventory.entity.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDetailsResponse {
    private String reservationId;
    private String productCode;
    private Integer quantity;
    private String initialNotes;
    private ReservationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime processedAt;
    private String notes;
    private Long timeToExpiryMinutes;
}
