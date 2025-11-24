package com.destore.finance.dto;

import com.destore.finance.entity.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinanceApprovalResponse {
    private String requestId;
    private RequestStatus status;
    private BigDecimal approvedAmount;
    private String message;
}
