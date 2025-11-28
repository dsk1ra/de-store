package com.destore.financeapproval.service;

import com.destore.dto.ApprovalDecisionMessage;
import com.destore.dto.EnablingRequest;
import com.destore.dto.EnablingResponse;
import com.destore.dto.PendingApprovalMessage;
import com.destore.financeapproval.config.ConfigurationPersistence;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Properties;

@Service
@Slf4j
public class FinanceApprovalAutomationService {
    
    private final ConfigurationPersistence configurationPersistence;
    private final RabbitTemplate rabbitTemplate;
    
    @Getter
    @Setter
    @Value("${enabling.approval.threshold:5000.00}")
    private BigDecimal approvalThreshold;
    
    @Getter
    @Setter
    @Value("${enabling.approval.processing-delay-ms:500}")
    private long processingDelayMs;
    
    @Getter
    @Setter
    @Value("${enabling.approval.auto-approve-enabled:true}")
    private boolean autoApproveEnabled;
    
    @Value("${rabbitmq.exchange.finance-approval}")
    private String financeApprovalExchange;
    
    @Value("${rabbitmq.routing-key.approval-decision}")
    private String approvalDecisionRoutingKey;
    
    public FinanceApprovalAutomationService(ConfigurationPersistence configurationPersistence, RabbitTemplate rabbitTemplate) {
        this.configurationPersistence = configurationPersistence;
        this.rabbitTemplate = rabbitTemplate;
    }
    
    @PostConstruct
    public void init() {
        // Load persisted configuration if available
        Properties props = configurationPersistence.loadConfiguration();
        
        if (props.containsKey("approvalThreshold")) {
            approvalThreshold = new BigDecimal(props.getProperty("approvalThreshold"));
            log.info("Loaded persisted approvalThreshold: {}", approvalThreshold);
        }
        
        if (props.containsKey("processingDelayMs")) {
            processingDelayMs = Long.parseLong(props.getProperty("processingDelayMs"));
            log.info("Loaded persisted processingDelayMs: {}", processingDelayMs);
        }
        
        if (props.containsKey("autoApproveEnabled")) {
            autoApproveEnabled = Boolean.parseBoolean(props.getProperty("autoApproveEnabled"));
            log.info("Loaded persisted autoApproveEnabled: {}", autoApproveEnabled);
        }
        
        log.info("Finance Approval Automation Service initialized with configuration:");
        log.info("  - Approval Threshold: £{}", approvalThreshold);
        log.info("  - Processing Delay: {} ms", processingDelayMs);
        log.info("  - Auto-Approve Enabled: {}", autoApproveEnabled);
    }
    
    public void persistConfiguration() {
        configurationPersistence.saveConfiguration(approvalThreshold, processingDelayMs, autoApproveEnabled);
    }
    
    public EnablingResponse processApproval(EnablingRequest request) {
        log.info("Processing approval request for customer: {}, amount: {}", 
                request.getCustomerId(), request.getAmount());
        
        // Check if auto-approve is disabled
        if (!autoApproveEnabled) {
            log.info("Auto-approve is disabled - rejecting request {}", request.getRequestId());
            return EnablingResponse.builder()
                    .requestId(request.getRequestId())
                    .approved(false)
                    .approvedAmount(BigDecimal.ZERO)
                    .reason("Rejected - Auto-approval is currently disabled")
                    .build();
        }
        
        // Simulate processing delay
        try {
            Thread.sleep(processingDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Simple approval logic: approve if amount < threshold
        boolean approved = request.getAmount().compareTo(approvalThreshold) < 0;
        
        String reason;
        if (approved) {
            reason = String.format("Approved - amount within automatic approval limit of £%.2f", approvalThreshold);
        } else {
            reason = String.format("Rejected - amount exceeds automatic approval limit of £%.2f", approvalThreshold);
        }
        
        log.info("Approval decision for request {}: {}", request.getRequestId(), 
                approved ? "APPROVED" : "REJECTED");
        
        return EnablingResponse.builder()
                .requestId(request.getRequestId())
                .approved(approved)
                .approvedAmount(approved ? request.getAmount() : BigDecimal.ZERO)
                .reason(reason)
                .build();
    }
    
    /**
     * Process a pending approval request from the queue.
     * This method is called when auto-approve is enabled and processes requests from the queue
     * based on the threshold amount.
     */
    public void processQueuedApproval(PendingApprovalMessage message) {
        log.info("Processing queued approval request for customer: {}, amount: {}", 
                message.getCustomerId(), message.getAmount());
        
        // Check if auto-approve is disabled
        if (!autoApproveEnabled) {
            log.info("Auto-approve is disabled - skipping queued request {}", message.getRequestId());
            return;
        }
        
        // Simulate processing delay
        try {
            log.info("Processing request {} with delay of {} ms", message.getRequestId(), processingDelayMs);
            Thread.sleep(processingDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Processing interrupted for request {}", message.getRequestId());
            return;
        }
        
        // Check amount against threshold: approve if <= threshold
        boolean approved = message.getAmount().compareTo(approvalThreshold) <= 0;
        
        String reason;
        String decision;
        if (approved) {
            decision = "APPROVED";
            reason = String.format("Approved by automation - amount £%.2f is within threshold of £%.2f", 
                    message.getAmount(), approvalThreshold);
        } else {
            decision = "DECLINED";
            reason = String.format("Rejected by automation - amount £%.2f exceeds threshold of £%.2f", 
                    message.getAmount(), approvalThreshold);
        }
        
        log.info("Approval decision for queued request {}: {} - {}", 
                message.getRequestId(), decision, reason);
        
        // Send decision back to approval decision queue
        ApprovalDecisionMessage decisionMessage = ApprovalDecisionMessage.builder()
                .requestId(message.getRequestId())
                .decision(decision)
                .decidedBy("FINANCE_APPROVAL_AUTOMATION")
                .notes(reason)
                .timestamp(LocalDateTime.now())
                .build();
        
        rabbitTemplate.convertAndSend(financeApprovalExchange, approvalDecisionRoutingKey, decisionMessage);
        log.info("Published {} decision for request {} to approval decision queue", 
                decision, message.getRequestId());
    }
}
