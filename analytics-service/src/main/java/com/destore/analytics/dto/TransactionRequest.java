package com.destore.analytics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class TransactionRequest {
    
    @NotBlank(message = "Transaction ID is required")
    private String transactionId;
    
    @NotBlank(message = "Customer ID is required")
    private String customerId;
    
    private String customerName;
    
    @NotBlank(message = "Store ID is required")
    private String storeId;
    
    @NotNull(message = "Total amount is required")
    private BigDecimal totalAmount;
    
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal netAmount;
    private String paymentMethod;
    private String transactionStatus;
    private String items;
    private LocalDateTime transactionDate;
}
