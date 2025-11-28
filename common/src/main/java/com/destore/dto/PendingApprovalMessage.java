package com.destore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Message DTO for pending finance approval requests.
 * Used for communication between finance-service and finance-approval-automation via RabbitMQ.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingApprovalMessage {
    private String requestId;
    private String customerId;
    private BigDecimal amount;
    private String purpose;
    private LocalDateTime timestamp;
}
