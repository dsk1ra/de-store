package com.destore.enabling.dto;

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
public class PendingApprovalMessage {
    private String requestId;
    private String customerId;
    private BigDecimal amount;
    private String purpose;
    private LocalDateTime timestamp;
}
