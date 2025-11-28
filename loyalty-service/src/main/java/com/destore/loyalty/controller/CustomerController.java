package com.destore.loyalty.controller;

import com.destore.dto.ApiResponse;
import com.destore.loyalty.dto.CustomerRequest;
import com.destore.loyalty.dto.CustomerResponse;
import com.destore.loyalty.dto.PurchaseRequest;
import com.destore.loyalty.entity.Customer;
import com.destore.loyalty.entity.LoyaltyTier;
import com.destore.loyalty.entity.PurchaseHistory;
import com.destore.loyalty.service.LoyaltyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/loyalty/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerController {
    
    private final LoyaltyService loyaltyService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponse>> registerCustomer(@Valid @RequestBody CustomerRequest request) {
        try {
            Customer customer = loyaltyService.registerCustomer(request);
            CustomerResponse response = CustomerResponse.fromEntity(customer);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(true, "Customer registered successfully", response));
        } catch (Exception e) {
            log.error("Error registering customer", e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    @GetMapping("/{customerId}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomer(@PathVariable String customerId) {
        try {
            Customer customer = loyaltyService.getCustomer(customerId);
            CustomerResponse response = CustomerResponse.fromEntity(customer);
            return ResponseEntity.ok(new ApiResponse<>(true, "Customer retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error retrieving customer", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> getAllCustomers() {
        try {
            List<Customer> customers = loyaltyService.getAllCustomers();
            List<CustomerResponse> responses = customers.stream()
                    .map(CustomerResponse::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(new ApiResponse<>(true, "Customers retrieved successfully", responses));
        } catch (Exception e) {
            log.error("Error retrieving customers", e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> getActiveCustomers() {
        try {
            List<Customer> customers = loyaltyService.getActiveCustomers();
            List<CustomerResponse> responses = customers.stream()
                    .map(CustomerResponse::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(new ApiResponse<>(true, "Active customers retrieved", responses));
        } catch (Exception e) {
            log.error("Error retrieving active customers", e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    @GetMapping("/regular")
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> getRegularCustomers() {
        try {
            List<Customer> customers = loyaltyService.getRegularCustomers();
            List<CustomerResponse> responses = customers.stream()
                    .map(CustomerResponse::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(new ApiResponse<>(true, "Regular customers retrieved", responses));
        } catch (Exception e) {
            log.error("Error retrieving regular customers", e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    @GetMapping("/tier/{tier}")
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> getCustomersByTier(@PathVariable LoyaltyTier tier) {
        try {
            List<Customer> customers = loyaltyService.getCustomersByTier(tier);
            List<CustomerResponse> responses = customers.stream()
                    .map(CustomerResponse::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(new ApiResponse<>(true, "Customers by tier retrieved", responses));
        } catch (Exception e) {
            log.error("Error retrieving customers by tier", e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    @PostMapping("/{customerId}/purchases")
    public ResponseEntity<ApiResponse<CustomerResponse>> recordPurchase(
            @PathVariable String customerId,
            @Valid @RequestBody PurchaseRequest request) {
        try {
            request.setCustomerId(customerId);
            LoyaltyService.CustomerWithPoints result = loyaltyService.recordPurchase(request);
            CustomerResponse response = CustomerResponse.fromEntity(result.customer(), result.pointsEarned());
            return ResponseEntity.ok(new ApiResponse<>(true, "Purchase recorded successfully", response));
        } catch (Exception e) {
            log.error("Error recording purchase", e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    @GetMapping("/{customerId}/history")
    public ResponseEntity<ApiResponse<List<PurchaseHistory>>> getPurchaseHistory(@PathVariable String customerId) {
        try {
            List<PurchaseHistory> history = loyaltyService.getCustomerPurchaseHistory(customerId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Purchase history retrieved", history));
        } catch (Exception e) {
            log.error("Error retrieving purchase history", e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    @PostMapping("/{customerId}/redeem")
    public ResponseEntity<ApiResponse<CustomerResponse>> redeemPoints(
            @PathVariable String customerId,
            @RequestParam Integer points) {
        try {
            Customer customer = loyaltyService.redeemPoints(customerId, points);
            CustomerResponse response = CustomerResponse.fromEntity(customer);
            return ResponseEntity.ok(new ApiResponse<>(true, "Points redeemed successfully", response));
        } catch (Exception e) {
            log.error("Error redeeming points", e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    @GetMapping("/{customerId}/discount")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCustomerDiscount(@PathVariable String customerId) {
        try {
            Customer customer = loyaltyService.getCustomer(customerId);
            int discount = customer.getDiscountPercentage();
            Map<String, Object> discountInfo = new java.util.HashMap<>();
            discountInfo.put("discountPercentage", discount);
            discountInfo.put("tier", customer.getLoyaltyTier().name());
            discountInfo.put("loyaltyPoints", customer.getLoyaltyPoints());
            return ResponseEntity.ok(new ApiResponse<>(true, "Discount calculated", discountInfo));
        } catch (Exception e) {
            log.error("Error calculating discount", e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    @PutMapping("/{customerId}")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(
            @PathVariable String customerId,
            @Valid @RequestBody CustomerRequest request) {
        try {
            Customer customer = loyaltyService.updateCustomer(customerId, request);
            CustomerResponse response = CustomerResponse.fromEntity(customer);
            return ResponseEntity.ok(new ApiResponse<>(true, "Customer updated successfully", response));
        } catch (Exception e) {
            log.error("Error updating customer", e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    @DeleteMapping("/{customerId}")
    public ResponseEntity<ApiResponse<Void>> deactivateCustomer(@PathVariable String customerId) {
        try {
            loyaltyService.deactivateCustomer(customerId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Customer deactivated successfully", null));
        } catch (Exception e) {
            log.error("Error deactivating customer", e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Loyalty Service - Customer Controller is running");
    }
}
