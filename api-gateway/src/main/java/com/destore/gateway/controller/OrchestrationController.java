package com.destore.gateway.controller;

import com.destore.gateway.dto.PurchaseRequest;
import com.destore.gateway.dto.PurchaseResponse;
import com.destore.gateway.service.OrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orchestration")
@RequiredArgsConstructor
@Slf4j
public class OrchestrationController {
    
    private final OrchestrationService orchestrationService;
    
    @PostMapping("/purchase")
    public ResponseEntity<PurchaseResponse> processPurchase(
            @RequestBody PurchaseRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authToken) {
        
        log.info("Received purchase request for customer: {}", request.getCustomerId());
        PurchaseResponse response = orchestrationService.processPurchase(request, authToken);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("API Gateway is running");
    }
}
