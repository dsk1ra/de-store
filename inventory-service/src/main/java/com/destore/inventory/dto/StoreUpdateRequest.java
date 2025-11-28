package com.destore.inventory.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating a store.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreUpdateRequest {
    
    @Size(max = 200, message = "Store name must not exceed 200 characters")
    private String name;
    
    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;
    
    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;
    
    @Size(max = 100, message = "Region must not exceed 100 characters")
    private String region;
    
    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    private String postalCode;
    
    @Size(max = 100, message = "Country must not exceed 100 characters")
    private String country;
    
    @Size(max = 50, message = "Phone must not exceed 50 characters")
    private String phone;
    
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;
    
    @Size(max = 100, message = "Manager name must not exceed 100 characters")
    private String managerName;
    
    private Boolean active;
}
