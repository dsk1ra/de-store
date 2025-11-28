package com.destore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event DTO for low stock notifications.
 * Published to RabbitMQ when inventory falls below threshold.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LowStockEvent {
    private String productCode;
    private int currentQuantity;
    private int threshold;
    private String storeId;
    private LocalDateTime timestamp;
}
