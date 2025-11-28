package com.destore.inventory.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Client for validating that products exist in the pricing service.
 * Products must exist before inventory can be created for them.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductValidationClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${services.pricing-service.url:http://localhost:8082}")
    private String pricingServiceUrl;
    
    /**
     * Checks if a product exists in the pricing service.
     * 
     * @param productCode the product code to check
     * @return true if the product exists, false otherwise
     */
    public boolean productExists(String productCode) {
        try {
            String url = pricingServiceUrl + "/api/pricing/products/" + productCode;
            
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> body = response.getBody();
                Boolean success = (Boolean) body.get("success");
                return success != null && success;
            }
            return false;
        } catch (Exception e) {
            log.warn("Failed to validate product {} (pricing service may be unavailable): {}", 
                    productCode, e.getMessage());
            return false;
        }
    }
    
    /**
     * Validates that a product exists.
     * 
     * @param productCode the product code to validate
     * @throws com.destore.exception.ResourceNotFoundException if the product doesn't exist
     */
    public void validateProductExists(String productCode) {
        if (!productExists(productCode)) {
            throw new com.destore.exception.ResourceNotFoundException(
                    "Product", productCode + " (product must be created in pricing service before creating inventory)");
        }
    }
}
