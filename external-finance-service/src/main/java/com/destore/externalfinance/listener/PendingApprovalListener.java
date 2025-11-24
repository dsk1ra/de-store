package com.destore.externalfinance.listener;

import com.destore.externalfinance.dto.PendingApprovalMessage;
import com.destore.externalfinance.service.ExternalFinanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PendingApprovalListener {
    
    private final ExternalFinanceService externalFinanceService;
    
    /**
     * Listens to the pending approval queue and processes requests when auto-approve is enabled.
     * When a request is received:
     * 1. Check if auto-approve is enabled
     * 2. If enabled, evaluate the request against the threshold
     * 3. Wait for the configured processing delay
     * 4. Send approval/rejection decision back to the approval decision queue
     */
    @RabbitListener(queues = "${rabbitmq.queue.pending-approval}")
    public void handlePendingApproval(PendingApprovalMessage message) {
        log.info("Received pending approval request from queue: {}", message.getRequestId());
        log.info("Request details - Customer: {}, Amount: £{}, Purpose: {}", 
                message.getCustomerId(), message.getAmount(), message.getPurpose());
        
        if (!externalFinanceService.isAutoApproveEnabled()) {
            log.info("Auto-approve is disabled. Request {} will remain in pending state until manually processed.", 
                    message.getRequestId());
            return;
        }
        
        log.info("Auto-approve is enabled. Processing request {} from queue...", message.getRequestId());
        externalFinanceService.processQueuedApproval(message);
    }
}
