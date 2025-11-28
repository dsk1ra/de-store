package com.destore.gateway.service;

import com.destore.gateway.dto.PurchaseRequest;
import com.destore.gateway.dto.PurchaseResponse;
import com.destore.gateway.event.PurchaseEvent;
import com.destore.gateway.publisher.PurchaseEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrchestrationService {
    
    private final RestTemplate restTemplate;
    private final PurchaseEventPublisher purchaseEventPublisher;
    private final ObjectMapper objectMapper;
    
    // Service URLs - In production, these should come from service discovery
    private static final String INVENTORY_SERVICE_URL = "http://localhost:8083";
    private static final String FINANCE_SERVICE_URL = "http://localhost:8084";
    private static final String PRICING_SERVICE_URL = "http://localhost:8082";
    private static final String LOYALTY_SERVICE_URL = "http://localhost:8086";
    private static final String DELIVERY_SERVICE_URL = "http://localhost:8088";
    private static final String INVENTORY_STORE_URL = "http://localhost:8083";
    
    public PurchaseResponse processPurchase(PurchaseRequest request, String authToken) {
        String orderId = UUID.randomUUID().toString();
        String storeId = request.getStoreId() != null ? request.getStoreId() : "STORE-001";
        Map<String, String> reservationIds = new HashMap<>();
        
        try {
            // Step 0: Validate or auto-register customer
            // In retail, customers can be registered on-the-fly during their first purchase
            log.info("Step 0: Checking customer {} registration status", request.getCustomerId());
            if (!validateCustomerExists(request.getCustomerId(), authToken)) {
                log.info("Customer {} not found, attempting auto-registration", request.getCustomerId());
                if (!autoRegisterCustomer(request.getCustomerId(), request.getCustomerName(), authToken)) {
                    return PurchaseResponse.builder()
                            .success(false)
                            .message("Failed to register customer: " + request.getCustomerId() + ". Please try again or register manually.")
                            .orderId(orderId)
                            .build();
                }
                log.info("Customer {} auto-registered successfully", request.getCustomerId());
            }
            
            // Step 0b: Validate that store exists
            // Stores must be pre-configured by administrators - they cannot be auto-created
            log.info("Step 0b: Validating store {} exists", storeId);
            if (!validateStoreExists(storeId, authToken)) {
                return PurchaseResponse.builder()
                        .success(false)
                        .message("Store not found: " + storeId + ". Stores must be configured by an administrator before processing orders.")
                        .orderId(orderId)
                        .build();
            }
            
            // Step 1: Validate inventory availability
            log.info("Step 1: Validating inventory for order {}", orderId);
            for (PurchaseRequest.PurchaseItem item : request.getItems()) {
                if (!checkInventory(item.getProductCode(), item.getQuantity(), authToken)) {
                    return PurchaseResponse.builder()
                            .success(false)
                            .message("Insufficient inventory for product: " + item.getProductCode())
                            .orderId(orderId)
                            .build();
                }
            }
            
            // Step 2: Calculate pricing with promotions
            log.info("Step 2: Calculating prices for order {}", orderId);
            Map<String, Object> pricingResult = calculatePricing(request.getItems(), authToken);
            BigDecimal subtotal = getBigDecimalFromMap(pricingResult, "subtotal");
            BigDecimal promotionalDiscount = getBigDecimalFromMap(pricingResult, "promotionalDiscount");
            BigDecimal pricingTotal = getBigDecimalFromMap(pricingResult, "finalTotal");
            @SuppressWarnings("unchecked")
            List<String> appliedPromotions = (List<String>) pricingResult.getOrDefault("appliedPromotions", new ArrayList<>());
            
            // Step 3: Get loyalty discount and customer info
            log.info("Step 3: Checking loyalty status for customer {}", request.getCustomerId());
            Map<String, Object> loyaltyInfo = getLoyaltyInfo(request.getCustomerId(), authToken);
            int loyaltyDiscountPercent = loyaltyInfo != null ? (int) loyaltyInfo.getOrDefault("discountPercentage", 0) : 0;
            String currentTier = loyaltyInfo != null ? (String) loyaltyInfo.getOrDefault("tier", "BRONZE") : "BRONZE";
            BigDecimal loyaltyDiscount = pricingTotal.multiply(BigDecimal.valueOf(loyaltyDiscountPercent))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal afterLoyaltyDiscount = pricingTotal.subtract(loyaltyDiscount);
            
            // Step 4: Calculate delivery charge if needed
            log.info("Step 4: Calculating delivery for order {}", orderId);
            BigDecimal deliveryCharge = BigDecimal.ZERO;
            String deliveryOrderId = null;
            String estimatedDelivery = null;
            
            if (Boolean.TRUE.equals(request.getRequiresDelivery())) {
                Map<String, Object> deliveryResult = calculateDelivery(
                        orderId, 
                        request.getCustomerId(),
                        request.getCustomerName(),
                        storeId,
                        afterLoyaltyDiscount,
                        request.getDeliveryDistance(),
                        request.getDeliveryAddress(),
                        Boolean.TRUE.equals(request.getIsExpressDelivery()),
                        authToken
                );
                if (deliveryResult != null) {
                    deliveryCharge = getBigDecimalFromMap(deliveryResult, "totalDeliveryCharge");
                    deliveryOrderId = (String) deliveryResult.get("orderId");
                    // Estimated delivery time calculation
                    estimatedDelivery = Boolean.TRUE.equals(request.getIsExpressDelivery()) ? 
                            "1 hour" : "3 hours";
                }
            }
            
            // Step 5: Calculate final total
            BigDecimal finalTotal = afterLoyaltyDiscount.add(deliveryCharge);
            
            // Use provided total if available, otherwise use calculated
            BigDecimal amountForApproval = request.getTotalAmount() != null ? 
                    request.getTotalAmount() : finalTotal;
            
            // Step 6: Request finance approval
            log.info("Step 5: Requesting finance approval for order {}", orderId);
            String financeRequestId = requestFinanceApproval(request.getCustomerId(), amountForApproval, authToken);
            
            if (financeRequestId == null) {
                return PurchaseResponse.builder()
                        .success(false)
                        .message("Finance approval rejected")
                        .orderId(orderId)
                        .subtotal(subtotal)
                        .promotionalDiscount(promotionalDiscount)
                        .loyaltyDiscount(loyaltyDiscount)
                        .deliveryCharge(deliveryCharge)
                        .finalTotal(finalTotal)
                        .appliedPromotions(appliedPromotions)
                        .build();
            }
            
            // Step 7: Reserve inventory
            log.info("Step 6: Reserving inventory for order {}", orderId);
            for (PurchaseRequest.PurchaseItem item : request.getItems()) {
                String reservationId = reserveInventory(item.getProductCode(), item.getQuantity(), orderId, authToken);
                reservationIds.put(item.getProductCode(), reservationId);
            }
            
            // Step 8: Confirm reservations (deduct from inventory)
            log.info("Step 7: Confirming reservations for order {}", orderId);
            for (Map.Entry<String, String> entry : reservationIds.entrySet()) {
                confirmReservation(entry.getValue(), authToken);
            }
            
            // Step 9: Record purchase in loyalty service
            log.info("Step 8: Recording purchase in loyalty service for customer {}", request.getCustomerId());
            Map<String, Object> loyaltyResult = recordLoyaltyPurchase(
                    request.getCustomerId(), 
                    orderId, 
                    finalTotal, 
                    buildItemsString(request.getItems()),
                    authToken
            );
            int pointsEarned = loyaltyResult != null ? (int) loyaltyResult.getOrDefault("pointsEarned", 0) : 0;
            int newPointsBalance = loyaltyResult != null ? (int) loyaltyResult.getOrDefault("loyaltyPoints", 0) : 0;
            String newTier = loyaltyResult != null ? (String) loyaltyResult.getOrDefault("tier", currentTier) : currentTier;
            
            // Step 10: Publish purchase event to analytics
            log.info("Step 9: Publishing purchase event for analytics");
            publishPurchaseEvent(orderId, request, finalTotal, promotionalDiscount.add(loyaltyDiscount), storeId);
            
            log.info("Purchase completed successfully for order {}", orderId);
            return PurchaseResponse.builder()
                    .success(true)
                    .message("Purchase completed successfully")
                    .orderId(orderId)
                    .financeRequestId(financeRequestId)
                    .subtotal(subtotal)
                    .promotionalDiscount(promotionalDiscount)
                    .loyaltyDiscount(loyaltyDiscount)
                    .deliveryCharge(deliveryCharge)
                    .finalTotal(finalTotal)
                    .appliedPromotions(appliedPromotions)
                    .loyaltyPointsEarned(pointsEarned)
                    .newLoyaltyPointsBalance(newPointsBalance)
                    .loyaltyTier(newTier)
                    .deliveryOrderId(deliveryOrderId)
                    .estimatedDeliveryTime(estimatedDelivery)
                    .build();
            
        } catch (Exception e) {
            log.error("Error processing purchase", e);
            
            // Compensation: Cancel any reservations that were made
            for (Map.Entry<String, String> entry : reservationIds.entrySet()) {
                try {
                    cancelReservation(entry.getValue(), authToken);
                    log.info("Cancelled reservation {} for product {} due to error", entry.getValue(), entry.getKey());
                } catch (Exception cancelEx) {
                    log.error("Failed to cancel reservation {}: {}", entry.getValue(), cancelEx.getMessage());
                }
            }
            
            return PurchaseResponse.builder()
                    .success(false)
                    .message("Error processing purchase: " + e.getMessage())
                    .orderId(orderId)
                    .build();
        }
    }
    
    /**
     * Validates that a customer exists in the loyalty service.
     * A customer must be registered before they can place an order.
     */
    private boolean validateCustomerExists(String customerId, String authToken) {
        try {
            String url = LOYALTY_SERVICE_URL + "/api/loyalty/customers/" + customerId;
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authToken);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> body = response.getBody();
                Boolean success = (Boolean) body.get("success");
                return success != null && success;
            }
            return false;
        } catch (Exception e) {
            log.warn("Customer validation failed (customer may not exist): {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Auto-registers a new customer during their first purchase.
     * This enables a seamless checkout experience while still maintaining customer records.
     * The customer starts at BRONZE tier and can earn loyalty points from this purchase.
     */
    private boolean autoRegisterCustomer(String customerId, String customerName, String authToken) {
        try {
            String url = LOYALTY_SERVICE_URL + "/api/loyalty/customers";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authToken);
            headers.set("Content-Type", "application/json");
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("customerId", customerId);
            requestBody.put("name", customerName != null ? customerName : "Customer " + customerId);
            requestBody.put("email", customerId + "@guest.destore.com"); // Placeholder email for auto-registered customers
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> body = response.getBody();
                Boolean success = (Boolean) body.get("success");
                return success != null && success;
            }
            return false;
        } catch (Exception e) {
            log.error("Failed to auto-register customer {}: {}", customerId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Validates that a store exists in the inventory service.
     * A store must be registered before orders can be placed for it.
     */
    private boolean validateStoreExists(String storeId, String authToken) {
        try {
            String url = INVENTORY_STORE_URL + "/api/stores/" + storeId + "/exists";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authToken);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> body = response.getBody();
                Boolean exists = (Boolean) body.get("data");
                return exists != null && exists;
            }
            return false;
        } catch (Exception e) {
            log.warn("Store validation failed (store may not exist): {}", e.getMessage());
            return false;
        }
    }
    
    private boolean checkInventory(String productCode, Integer quantity, String authToken) {
        try {
            String url = INVENTORY_SERVICE_URL + "/api/inventory/" + productCode;
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authToken);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            @SuppressWarnings({"rawtypes", "null"})
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> body = response.getBody();
                if (body != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) body.get("data");
                    Integer available = (Integer) data.get("quantity");
                    return available >= quantity;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("Error checking inventory", e);
            return false;
        }
    }
    
    private Map<String, Object> calculatePricing(List<PurchaseRequest.PurchaseItem> items, String authToken) {
        try {
            String url = PRICING_SERVICE_URL + "/api/pricing/calculate";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authToken);
            headers.set("Content-Type", "application/json");
            
            List<Map<String, Object>> itemsList = new ArrayList<>();
            for (PurchaseRequest.PurchaseItem item : items) {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("productCode", item.getProductCode());
                itemMap.put("quantity", item.getQuantity());
                itemsList.add(itemMap);
            }
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("items", itemsList);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> body = response.getBody();
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) body.get("data");
                return data;
            }
            return new HashMap<>();
        } catch (Exception e) {
            log.warn("Error calculating pricing (service may be unavailable): {}", e.getMessage());
            // Return empty map, the orchestration will use provided totalAmount
            return new HashMap<>();
        }
    }
    
    private Map<String, Object> getLoyaltyInfo(String customerId, String authToken) {
        try {
            String url = LOYALTY_SERVICE_URL + "/api/loyalty/customers/" + customerId + "/discount";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authToken);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> body = response.getBody();
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) body.get("data");
                return data;
            }
            return null;
        } catch (Exception e) {
            log.warn("Error getting loyalty info (customer may not exist or service unavailable): {}", e.getMessage());
            return null;
        }
    }
    
    private Map<String, Object> calculateDelivery(String orderId, String customerId, String customerName,
                                                    String storeId, BigDecimal orderValue, BigDecimal distance,
                                                    String deliveryAddress, boolean isExpress, String authToken) {
        try {
            String url = DELIVERY_SERVICE_URL + "/api/delivery/calculate";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authToken);
            headers.set("Content-Type", "application/json");
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("orderId", orderId);
            requestBody.put("customerId", customerId);
            requestBody.put("customerName", customerName);
            requestBody.put("storeId", storeId);
            requestBody.put("orderValue", orderValue);
            requestBody.put("distance", distance != null ? distance : BigDecimal.valueOf(5.0));
            requestBody.put("deliveryAddress", deliveryAddress != null ? deliveryAddress : "Customer Address");
            requestBody.put("isExpress", isExpress);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> body = response.getBody();
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) body.get("data");
                return data;
            }
            return null;
        } catch (Exception e) {
            log.warn("Error calculating delivery (service may be unavailable): {}", e.getMessage());
            return null;
        }
    }
    
    private Map<String, Object> recordLoyaltyPurchase(String customerId, String orderId, 
                                                        BigDecimal amount, String items, String authToken) {
        try {
            String url = LOYALTY_SERVICE_URL + "/api/loyalty/customers/" + customerId + "/purchases";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authToken);
            headers.set("Content-Type", "application/json");
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("customerId", customerId);
            requestBody.put("orderId", orderId);
            requestBody.put("amount", amount);
            requestBody.put("items", items);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> body = response.getBody();
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) body.get("data");
                return data;
            }
            return null;
        } catch (Exception e) {
            log.warn("Error recording loyalty purchase (customer may not exist): {}", e.getMessage());
            return null;
        }
    }
    
    private void publishPurchaseEvent(String orderId, PurchaseRequest request, 
                                        BigDecimal totalAmount, BigDecimal discount, String storeId) {
        try {
            PurchaseEvent event = PurchaseEvent.builder()
                    .orderId(orderId)
                    .customerId(request.getCustomerId())
                    .customerName(request.getCustomerName())
                    .storeId(storeId)
                    .totalAmount(totalAmount)
                    .discountAmount(discount)
                    .taxAmount(BigDecimal.ZERO)
                    .netAmount(totalAmount)
                    .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "CARD")
                    .transactionStatus("COMPLETED")
                    .items(buildItemsString(request.getItems()))
                    .transactionDate(LocalDateTime.now())
                    .build();
            
            purchaseEventPublisher.publishPurchaseEvent(event);
        } catch (Exception e) {
            log.error("Error publishing purchase event: {}", e.getMessage());
            // Don't fail the purchase if event publishing fails
        }
    }
    
    private String buildItemsString(List<PurchaseRequest.PurchaseItem> items) {
        StringBuilder sb = new StringBuilder();
        for (PurchaseRequest.PurchaseItem item : items) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(item.getProductCode()).append(" x").append(item.getQuantity());
        }
        return sb.toString();
    }
    
    private String requestFinanceApproval(String customerId, java.math.BigDecimal amount, String authToken) {
        try {
            String url = FINANCE_SERVICE_URL + "/api/finance/approve";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authToken);
            headers.set("Content-Type", "application/json");
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("customerId", customerId);
            requestBody.put("amount", amount);
            requestBody.put("purpose", "Purchase");
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> body = response.getBody();
                if (body != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) body.get("data");
                    String status = (String) data.get("status");
                    if ("APPROVED".equals(status)) {
                        return (String) data.get("requestId");
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Error requesting finance approval", e);
            return null;
        }
    }
    
    private String reserveInventory(String productCode, Integer quantity, String orderId, String authToken) {
        try {
            String url = INVENTORY_SERVICE_URL + "/api/inventory/" + productCode + "/reserve";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authToken);
            headers.set("Content-Type", "application/json");
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("quantity", quantity);
            requestBody.put("notes", "Reserved for order: " + orderId);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> body = response.getBody();
                if (body != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) body.get("data");
                    return (String) data.get("reservationId");
                }
            }
            throw new RuntimeException("Failed to get reservation ID");
        } catch (Exception e) {
            log.error("Error reserving inventory", e);
            throw new RuntimeException("Failed to reserve inventory: " + e.getMessage());
        }
    }
    
    private void confirmReservation(String reservationId, String authToken) {
        try {
            String url = INVENTORY_SERVICE_URL + "/api/inventory/reservations/confirm";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authToken);
            headers.set("Content-Type", "application/json");
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("reservationId", reservationId);
            requestBody.put("notes", "Confirmed via purchase workflow");
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            restTemplate.postForEntity(url, entity, String.class);
            log.info("Successfully confirmed reservation {}", reservationId);
        } catch (Exception e) {
            log.error("Error confirming reservation {}", reservationId, e);
            throw new RuntimeException("Failed to confirm reservation: " + e.getMessage());
        }
    }
    
    private void cancelReservation(String reservationId, String authToken) {
        try {
            String url = INVENTORY_SERVICE_URL + "/api/inventory/reservations/cancel";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authToken);
            headers.set("Content-Type", "application/json");
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("reservationId", reservationId);
            requestBody.put("notes", "Cancelled due to purchase workflow failure");
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            restTemplate.postForEntity(url, entity, String.class);
            log.info("Successfully cancelled reservation {}", reservationId);
        } catch (Exception e) {
            log.error("Error cancelling reservation {}", reservationId, e);
        }
    }
    
    private BigDecimal getBigDecimalFromMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        if (value instanceof String) return new BigDecimal((String) value);
        return BigDecimal.ZERO;
    }
}
