package com.destore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO from the external "Enabling" finance approval system.
 * Used for communication between finance-service and finance-approval-automation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnablingResponse {
    private String requestId;
    private boolean approved;
    private BigDecimal approvedAmount;
    private String reason;
}
