package com.destore.enabling.service;

import com.destore.enabling.dto.ApprovalRequest;
import com.destore.enabling.dto.ApprovalResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class EnablingSimulatorService {
    
    private static final BigDecimal APPROVAL_THRESHOLD = new BigDecimal("5000.00");
    
    public ApprovalResponse processApproval(ApprovalRequest request) {
        log.info("Processing approval request for customer: {}, amount: {}", 
                request.getCustomerId(), request.getAmount());
        
        // Simulate processing delay
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Simple approval logic: approve if amount < $5000
        boolean approved = request.getAmount().compareTo(APPROVAL_THRESHOLD) < 0;
        
        String reason;
        if (approved) {
            reason = "Approved - amount within automatic approval limit";
        } else {
            reason = "Rejected - amount exceeds automatic approval limit of $5000";
        }
        
        log.info("Approval decision for request {}: {}", request.getRequestId(), 
                approved ? "APPROVED" : "REJECTED");
        
        return ApprovalResponse.builder()
                .requestId(request.getRequestId())
                .approved(approved)
                .approvedAmount(approved ? request.getAmount() : BigDecimal.ZERO)
                .reason(reason)
                .build();
    }
}
