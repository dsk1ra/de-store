package com.destore.finance.listener;

import com.destore.finance.dto.ApprovalDecisionMessage;
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

    // NOTE: The pending-approval queue is now consumed by the
    // external-finance-service
    // When auto-approve is enabled, the simulator will process requests from the
    // queue
    // and send decisions back to the approval-decision queue

    @RabbitListener(queues = "${rabbitmq.queue.approval-decision}")
    public void handleApprovalDecision(ApprovalDecisionMessage decision) {
        log.info("Received approval decision from queue: {} for request {}",
                decision.getDecision(), decision.getRequestId());

        try {
            if ("APPROVED".equals(decision.getDecision())) {
                financeIntegrationService.processApprovalDecision(
                        decision.getRequestId(),
                        decision.getDecidedBy(),
                        decision.getNotes());
                log.info("Successfully processed approval for request {}", decision.getRequestId());
            } else if ("DECLINED".equals(decision.getDecision())) {
                financeIntegrationService.processDeclineDecision(
                        decision.getRequestId(),
                        decision.getDecidedBy(),
                        decision.getNotes());
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
