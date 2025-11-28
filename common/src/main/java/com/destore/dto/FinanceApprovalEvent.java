package com.destore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event DTO for finance approval notifications.
 * Published to RabbitMQ when a finance request is approved or rejected.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinanceApprovalEvent {
    private String requestId;
    private String customerEmail;
    private BigDecimal purchaseAmount;
    private String approvalCode;
    private LocalDateTime timestamp;
}
