package com.destore.loyalty.dto;

import com.destore.loyalty.entity.Customer;
import com.destore.loyalty.entity.LoyaltyTier;
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
public class CustomerResponse {
    private Long id;
    private String customerId;
    private String name;
    private String email;
    private String phone;
    private String address;
    private Integer loyaltyPoints;
    private LoyaltyTier loyaltyTier;
    private BigDecimal totalSpent;
    private Integer purchaseCount;
    private LocalDateTime joinedDate;
    private LocalDateTime lastPurchaseDate;
    private Boolean active;
    private Boolean isRegularCustomer;
    private Integer discountPercentage;
    private Integer pointsEarned; // Points earned from last purchase
    
    public static CustomerResponse fromEntity(Customer customer) {
        return fromEntity(customer, null);
    }
    
    public static CustomerResponse fromEntity(Customer customer, Integer pointsEarned) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .customerId(customer.getCustomerId())
                .name(customer.getName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .address(customer.getAddress())
                .loyaltyPoints(customer.getLoyaltyPoints())
                .loyaltyTier(customer.getLoyaltyTier())
                .totalSpent(customer.getTotalSpent())
                .purchaseCount(customer.getPurchaseCount())
                .joinedDate(customer.getJoinedDate())
                .lastPurchaseDate(customer.getLastPurchaseDate())
                .active(customer.getActive())
                .isRegularCustomer(customer.isRegularCustomer())
                .discountPercentage(customer.getDiscountPercentage())
                .pointsEarned(pointsEarned)
                .build();
    }
}
