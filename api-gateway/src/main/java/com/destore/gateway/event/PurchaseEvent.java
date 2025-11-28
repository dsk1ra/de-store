package com.destore.gateway.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event published to RabbitMQ when a purchase is completed successfully.
 * This event is consumed by analytics-service to track sales transactions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseEvent {
    
    private String orderId;
    private String customerId;
    private String customerName;
    private String storeId;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal netAmount;
    private String paymentMethod;
    private String transactionStatus;
    private String items;
    private LocalDateTime transactionDate;
}
