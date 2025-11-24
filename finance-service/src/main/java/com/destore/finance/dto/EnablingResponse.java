package com.destore.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

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
