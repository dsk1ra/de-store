package com.destore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for the external "Enabling" finance approval system.
 * Used for communication between finance-service and finance-approval-automation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnablingRequest {
    private String customerId;
    private BigDecimal amount;
    private String requestId;
}
