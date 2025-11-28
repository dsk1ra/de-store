package com.destore.financeapproval.controller;

import com.destore.dto.EnablingRequest;
import com.destore.dto.EnablingResponse;
import com.destore.financeapproval.service.FinanceApprovalAutomationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/finance-approval")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class FinanceApprovalAutomationController {
    
    private final FinanceApprovalAutomationService financeApprovalAutomationService;
    private final RestTemplate restTemplate;
    
    @Value("${finance.service.url}")
    private String financeServiceUrl;
    
    @PostMapping("/approve")
    public ResponseEntity<EnablingResponse> approveRequest(@RequestBody EnablingRequest request) {
        log.info("Received approval request: {}", request.getRequestId());
        EnablingResponse response = financeApprovalAutomationService.processApproval(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Finance Approval Automation Service is running");
    }
    
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("approvalThreshold", financeApprovalAutomationService.getApprovalThreshold());
        config.put("processingDelayMs", financeApprovalAutomationService.getProcessingDelayMs());
        config.put("autoApproveEnabled", financeApprovalAutomationService.isAutoApproveEnabled());
        log.info("Retrieved finance approval automation service configuration");
        return ResponseEntity.ok(config);
    }
    
    @PutMapping("/config")
    public ResponseEntity<Map<String, Object>> updateConfig(@RequestBody Map<String, Object> configUpdates) {
        log.info("Updating finance approval automation service configuration: {}", configUpdates);
        
        if (configUpdates.containsKey("approvalThreshold")) {
            Object thresholdValue = configUpdates.get("approvalThreshold");
            BigDecimal threshold = new BigDecimal(thresholdValue.toString());
            financeApprovalAutomationService.setApprovalThreshold(threshold);
            log.info("Updated approval threshold to: {}", threshold);
        }
        
        if (configUpdates.containsKey("processingDelayMs")) {
            Object delayValue = configUpdates.get("processingDelayMs");
            long delay = ((Number) delayValue).longValue();
            financeApprovalAutomationService.setProcessingDelayMs(delay);
            log.info("Updated processing delay to: {} ms", delay);
        }
        
        if (configUpdates.containsKey("autoApproveEnabled")) {
            boolean wasEnabled = financeApprovalAutomationService.isAutoApproveEnabled();
            boolean enabled = (Boolean) configUpdates.get("autoApproveEnabled");
            financeApprovalAutomationService.setAutoApproveEnabled(enabled);
            log.info("Updated auto-approve enabled to: {}", enabled);
            
            // If auto-approve was just enabled, trigger reprocessing of pending requests
            if (!wasEnabled && enabled) {
                log.info("Auto-approve was just enabled. Triggering reprocessing of pending requests...");
                try {
                    String reprocessUrl = financeServiceUrl + "/api/finance/reprocess-pending";
                    ResponseEntity<Map> response = restTemplate.postForEntity(reprocessUrl, null, Map.class);
                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        Map<String, Object> responseBody = response.getBody();
                        Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                        if (data != null) {
                            Integer reprocessedCount = (Integer) data.get("reprocessedCount");
                            log.info("Successfully triggered reprocessing of {} pending request(s)", reprocessedCount);
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to trigger reprocessing of pending requests", e);
                }
            }
        }
        
        // Persist the configuration changes
        financeApprovalAutomationService.persistConfiguration();
        log.info("Configuration changes persisted to file");
        
        // Return updated config
        Map<String, Object> config = new HashMap<>();
        config.put("approvalThreshold", financeApprovalAutomationService.getApprovalThreshold());
        config.put("processingDelayMs", financeApprovalAutomationService.getProcessingDelayMs());
        config.put("autoApproveEnabled", financeApprovalAutomationService.isAutoApproveEnabled());
        config.put("message", "Configuration updated and persisted successfully");
        
        return ResponseEntity.ok(config);
    }
}
