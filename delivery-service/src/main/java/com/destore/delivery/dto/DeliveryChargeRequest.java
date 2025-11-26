package com.destore.delivery.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryChargeRequest {
    
    @NotBlank(message = "Order ID is required")
    private String orderId;
    
    @NotBlank(message = "Customer ID is required")
    private String customerId;
    
    private String customerName;
    
    @NotBlank(message = "Store ID is required")
    private String storeId;
    
    @NotNull(message = "Order value is required")
    @DecimalMin(value = "0.01", message = "Order value must be positive")
    private BigDecimal orderValue;
    
    @NotNull(message = "Distance is required")
    @DecimalMin(value = "0.0", message = "Distance cannot be negative")
    private BigDecimal distance;  // in kilometers
    
    @NotBlank(message = "Delivery address is required")
    private String deliveryAddress;
    
    private String pickupAddress;
    
    @NotNull(message = "Express delivery flag is required")
    private Boolean isExpress;
    
    private String notes;
}
