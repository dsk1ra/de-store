package com.destore.delivery.dto;

import com.destore.delivery.entity.DeliveryStatus;
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
public class DeliveryOrderResponse {
    
    private Long id;
    private String orderId;
    private String customerId;
    private String customerName;
    private String storeId;
    private BigDecimal orderValue;
    private BigDecimal deliveryCharge;
    private BigDecimal distance;
    private String zone;
    private Boolean isExpress;
    private DeliveryStatus status;
    private String deliveryAddress;
    private LocalDateTime orderDateTime;
    private LocalDateTime estimatedDeliveryTime;
    private LocalDateTime actualDeliveryTime;
    private String driverName;
    private String driverPhone;
    private String vehicleNumber;
    private String notes;
}
