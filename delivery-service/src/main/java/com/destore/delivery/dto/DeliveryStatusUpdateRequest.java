package com.destore.delivery.dto;

import com.destore.delivery.entity.DeliveryStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryStatusUpdateRequest {
    
    @NotBlank(message = "Order ID is required")
    private String orderId;
    
    @NotBlank(message = "Status is required")
    private DeliveryStatus status;
    
    private String driverName;
    private String driverPhone;
    private String vehicleNumber;
    private String notes;
}
