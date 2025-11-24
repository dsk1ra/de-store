package com.destore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LowStockEvent {
    private String eventType;
    private String productCode;
    private String productName;
    private int currentQuantity;
    private int threshold;
    private String storeId;
    private String severity;
    private LocalDateTime timestamp;
}
