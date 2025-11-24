package com.destore.gateway.service;

import com.destore.gateway.dto.PurchaseRequest;
import com.destore.gateway.dto.PurchaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrchestrationService {
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    public PurchaseResponse processPurchase(PurchaseRequest request, String authToken) {
        String orderId = UUID.randomUUID().toString();
        
        try {
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
            
            // Step 2: Request finance approval
            log.info("Step 2: Requesting finance approval for order {}", orderId);
            String financeRequestId = requestFinanceApproval(request.getCustomerId(), request.getTotalAmount(), authToken);
            
            if (financeRequestId == null) {
                return PurchaseResponse.builder()
                        .success(false)
                        .message("Finance approval rejected")
                        .orderId(orderId)
                        .build();
            }
            
            // Step 3: Reserve inventory
            log.info("Step 3: Reserving inventory for order {}", orderId);
            for (PurchaseRequest.PurchaseItem item : request.getItems()) {
                reserveInventory(item.getProductCode(), item.getQuantity(), orderId, authToken);
            }
            
            // Step 4: Deduct inventory
            log.info("Step 4: Deducting inventory for order {}", orderId);
            for (PurchaseRequest.PurchaseItem item : request.getItems()) {
                deductInventory(item.getProductCode(), item.getQuantity(), authToken);
            }
            
            log.info("Purchase completed successfully for order {}", orderId);
            return PurchaseResponse.builder()
                    .success(true)
                    .message("Purchase completed successfully")
                    .orderId(orderId)
                    .financeRequestId(financeRequestId)
                    .build();
            
        } catch (Exception e) {
            log.error("Error processing purchase", e);
            return PurchaseResponse.builder()
                    .success(false)
                    .message("Error processing purchase: " + e.getMessage())
                    .orderId(orderId)
                    .build();
        }
    }
    
    private boolean checkInventory(String productCode, Integer quantity, String authToken) {
        try {
            String url = "http://localhost:8083/api/inventory/" + productCode;
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
    
    private String requestFinanceApproval(String customerId, java.math.BigDecimal amount, String authToken) {
        try {
            String url = "http://localhost:8084/api/finance/approve";
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
    
    private void reserveInventory(String productCode, Integer quantity, String orderId, String authToken) {
        try {
            String url = "http://localhost:8083/api/inventory/" + productCode + "/reserve";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authToken);
            headers.set("Content-Type", "application/json");
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("quantity", quantity);
            requestBody.put("referenceId", orderId);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            restTemplate.postForEntity(url, entity, String.class);
        } catch (Exception e) {
            log.error("Error reserving inventory", e);
            throw new RuntimeException("Failed to reserve inventory");
        }
    }
    
    private void deductInventory(String productCode, Integer quantity, String authToken) {
        try {
            String url = "http://localhost:8083/api/inventory/" + productCode + "/deduct?quantity=" + quantity;
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authToken);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            restTemplate.postForEntity(url, entity, String.class);
        } catch (Exception e) {
            log.error("Error deducting inventory", e);
            throw new RuntimeException("Failed to deduct inventory");
        }
    }
}
