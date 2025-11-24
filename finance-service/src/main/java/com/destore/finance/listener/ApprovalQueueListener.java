package com.destore.finance.listener;

import com.destore.finance.dto.ApprovalDecisionMessage;
import com.destore.finance.dto.PendingApprovalMessage;
import com.destore.finance.service.FinanceIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApprovalQueueListener {
    
    private final FinanceIntegrationService financeIntegrationService;
    
    @RabbitListener(queues = "${rabbitmq.queue.pending-approval}")
    public void handlePendingApproval(PendingApprovalMessage message) {
        log.info("Received pending approval request from queue: {}", message.getRequestId());
        log.info("Request details - Customer: {}, Amount: {}, Purpose: {}", 
                message.getCustomerId(), message.getAmount(), message.getPurpose());
        
        // Requests now wait in queue for manual approval
        // DO NOT auto-approve - wait for manual decision via /approve or /decline endpoints
        log.info("Request {} is waiting in queue for manual approval or decline", message.getRequestId());
        log.info("Use PUT /api/finance/approve/{} to approve or PUT /api/finance/decline/{} to reject", 
                message.getRequestId(), message.getRequestId());
    }
    
    @RabbitListener(queues = "${rabbitmq.queue.approval-decision}")
    public void handleApprovalDecision(ApprovalDecisionMessage decision) {
        log.info("Received approval decision from queue: {} for request {}", 
                decision.getDecision(), decision.getRequestId());
        
        try {
            if ("APPROVED".equals(decision.getDecision())) {
                financeIntegrationService.processApprovalDecision(
                        decision.getRequestId(), 
                        decision.getDecidedBy(), 
                        decision.getNotes()
                );
                log.info("Successfully processed approval for request {}", decision.getRequestId());
            } else if ("DECLINED".equals(decision.getDecision())) {
                financeIntegrationService.processDeclineDecision(
                        decision.getRequestId(), 
                        decision.getDecidedBy(), 
                        decision.getNotes()
                );
                log.info("Successfully processed decline for request {}", decision.getRequestId());
            } else {
                log.error("Unknown decision type: {}", decision.getDecision());
            }
        } catch (Exception e) {
            log.error("Error processing approval decision for request {}", decision.getRequestId(), e);
            // In production, you might want to send this to a dead letter queue
        }
    }
}
