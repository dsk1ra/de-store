package com.destore.enabling.controller;

import com.destore.enabling.dto.ApprovalRequest;
import com.destore.enabling.dto.ApprovalResponse;
import com.destore.enabling.service.EnablingSimulatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/enabling")
@RequiredArgsConstructor
@Slf4j
public class EnablingSimulatorController {
    
    private final EnablingSimulatorService enablingSimulatorService;
    
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
}
