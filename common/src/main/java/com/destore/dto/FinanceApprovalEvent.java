package com.destore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinanceApprovalEvent {
    private String eventType;
    private String requestId;
    private String customerName;
    private String customerEmail;
    private BigDecimal purchaseAmount;
    private String approvalCode;
    private LocalDateTime timestamp;
}
