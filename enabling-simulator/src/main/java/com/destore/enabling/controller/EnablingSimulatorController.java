package com.destore.enabling.controller;

import com.destore.enabling.dto.ApprovalRequest;
import com.destore.enabling.dto.ApprovalResponse;
import com.destore.enabling.service.EnablingSimulatorService;
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
@RequestMapping("/api/enabling")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class EnablingSimulatorController {
    
    private final EnablingSimulatorService enablingSimulatorService;
    private final RestTemplate restTemplate;
    
    @Value("${finance.service.url}")
    private String financeServiceUrl;
    
    @PostMapping("/approve")
    public ResponseEntity<ApprovalResponse> approveRequest(@RequestBody ApprovalRequest request) {
        log.info("Received approval request: {}", request.getRequestId());
        ApprovalResponse response = enablingSimulatorService.processApproval(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Enabling Simulator is running");
    }
    
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("approvalThreshold", enablingSimulatorService.getApprovalThreshold());
        config.put("processingDelayMs", enablingSimulatorService.getProcessingDelayMs());
        config.put("autoApproveEnabled", enablingSimulatorService.isAutoApproveEnabled());
        log.info("Retrieved simulator configuration");
        return ResponseEntity.ok(config);
    }
    
    @PutMapping("/config")
    public ResponseEntity<Map<String, Object>> updateConfig(@RequestBody Map<String, Object> configUpdates) {
        log.info("Updating simulator configuration: {}", configUpdates);
        
        if (configUpdates.containsKey("approvalThreshold")) {
            Object thresholdValue = configUpdates.get("approvalThreshold");
            BigDecimal threshold = new BigDecimal(thresholdValue.toString());
            enablingSimulatorService.setApprovalThreshold(threshold);
            log.info("Updated approval threshold to: {}", threshold);
        }
        
        if (configUpdates.containsKey("processingDelayMs")) {
            Object delayValue = configUpdates.get("processingDelayMs");
            long delay = ((Number) delayValue).longValue();
            enablingSimulatorService.setProcessingDelayMs(delay);
            log.info("Updated processing delay to: {} ms", delay);
        }
        
        if (configUpdates.containsKey("autoApproveEnabled")) {
            boolean wasEnabled = enablingSimulatorService.isAutoApproveEnabled();
            boolean enabled = (Boolean) configUpdates.get("autoApproveEnabled");
            enablingSimulatorService.setAutoApproveEnabled(enabled);
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
        enablingSimulatorService.persistConfiguration();
        log.info("Configuration changes persisted to file");
        
        // Return updated config
        Map<String, Object> config = new HashMap<>();
        config.put("approvalThreshold", enablingSimulatorService.getApprovalThreshold());
        config.put("processingDelayMs", enablingSimulatorService.getProcessingDelayMs());
        config.put("autoApproveEnabled", enablingSimulatorService.isAutoApproveEnabled());
        config.put("message", "Configuration updated and persisted successfully");
        
        return ResponseEntity.ok(config);
    }
}
