package com.destore.finance.service;

import com.destore.dto.FinanceApprovalEvent;
import com.destore.finance.dto.*;
import com.destore.finance.entity.FinanceRequest;
import com.destore.finance.entity.RequestStatus;
import com.destore.finance.repository.FinanceRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinanceIntegrationService {
    
    private final FinanceRequestRepository financeRequestRepository;
    private final RestTemplate restTemplate;
    private final RabbitTemplate rabbitTemplate;
    
    @Value("${enabling.service.url}")
    private String enablingServiceUrl;
    
    @Value("${rabbitmq.exchange.finance-approval}")
    private String financeApprovalExchange;
    
    @Value("${rabbitmq.routing-key.finance-approval}")
    private String financeApprovalRoutingKey;
    
    @Value("${rabbitmq.routing-key.pending-approval}")
    private String pendingApprovalRoutingKey;
    
    @Transactional
    public FinanceApprovalResponse requestApproval(FinanceApprovalRequest request) {
        // Create finance request record in PENDING status
        String requestId = UUID.randomUUID().toString();
        FinanceRequest financeRequest = FinanceRequest.builder()
                .requestId(requestId)
                .customerId(request.getCustomerId())
                .amount(request.getAmount())
                .status(RequestStatus.PENDING)
                .approvalNotes("Request submitted: " + request.getPurpose())
                .build();
        
        @SuppressWarnings("null")
        FinanceRequest saved = financeRequestRepository.save(financeRequest);
        
        log.info("Created finance request {} for customer {} with amount {}", requestId, request.getCustomerId(), request.getAmount());
        
        // Publish to pending approval queue
        PendingApprovalMessage message = PendingApprovalMessage.builder()
                .requestId(requestId)
                .customerId(request.getCustomerId())
                .amount(request.getAmount())
                .purpose(request.getPurpose())
                .timestamp(LocalDateTime.now())
                .build();
        
        rabbitTemplate.convertAndSend(financeApprovalExchange, pendingApprovalRoutingKey, message);
        log.info("Published pending approval request {} to queue", requestId);
        
        // Return immediately with PENDING status
        return FinanceApprovalResponse.builder()
                .requestId(requestId)
                .status(RequestStatus.PENDING)
                .approvedAmount(null)
                .message("Finance request created and added to approval queue")
                .build();
    }
    
    @Value("${rabbitmq.routing-key.approval-decision}")
    private String approvalDecisionRoutingKey;
    
    @Transactional
    public FinanceApprovalResponse approveRequest(String requestId, String approvedBy, String notes) {
        FinanceRequest financeRequest = financeRequestRepository.findByRequestId(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Finance request not found: " + requestId));
        
        if (financeRequest.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("Request " + requestId + " is not in PENDING status. Current status: " + financeRequest.getStatus());
        }
        
        // Publish approval decision to queue for processing
        ApprovalDecisionMessage decision = ApprovalDecisionMessage.builder()
                .requestId(requestId)
                .decision("APPROVED")
                .decidedBy(approvedBy)
                .notes(notes)
                .timestamp(LocalDateTime.now())
                .build();
        
        rabbitTemplate.convertAndSend(financeApprovalExchange, approvalDecisionRoutingKey, decision);
        log.info("Published approval decision for request {} to queue", requestId);
        
        return FinanceApprovalResponse.builder()
                .requestId(requestId)
                .status(RequestStatus.PENDING)
                .approvedAmount(null)
                .message("Approval decision queued for processing")
                .build();
    }
    
    @Transactional
    public void processApprovalDecision(String requestId, String approvedBy, String notes) {
        FinanceRequest financeRequest = financeRequestRepository.findByRequestId(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Finance request not found: " + requestId));
        
        if (financeRequest.getStatus() != RequestStatus.PENDING) {
            log.warn("Request {} is not in PENDING status. Current status: {}", requestId, financeRequest.getStatus());
            return;
        }
        
        try {
            // Call Enabling system
            EnablingRequest enablingRequest = EnablingRequest.builder()
                    .customerId(financeRequest.getCustomerId())
                    .amount(financeRequest.getAmount())
                    .requestId(requestId)
                    .build();
            
            String url = enablingServiceUrl + "/api/enabling/approve";
            EnablingResponse enablingResponse = restTemplate.postForObject(url, enablingRequest, EnablingResponse.class);
            
            if (enablingResponse != null && enablingResponse.isApproved()) {
                // Update status to approved
                financeRequest.setStatus(RequestStatus.APPROVED);
                financeRequest.setExternalReferenceId(enablingResponse.getRequestId());
                financeRequest.setApprovalNotes("Approved by: " + approvedBy + ". " + (notes != null ? notes : "") + ". Enabling: " + enablingResponse.getReason());
                financeRequestRepository.save(financeRequest);
                
                // Publish approval event
                publishApprovalEvent(financeRequest, true, enablingResponse.getReason());
                
                log.info("Processed approval for finance request {} by {}", requestId, approvedBy);
            } else {
                // Enabling system rejected
                financeRequest.setStatus(RequestStatus.REJECTED);
                financeRequest.setApprovalNotes("Rejected by Enabling system: " + (enablingResponse != null ? enablingResponse.getReason() : "Unknown reason"));
                financeRequestRepository.save(financeRequest);
                
                log.info("Enabling system rejected request {}", requestId);
            }
        } catch (Exception e) {
            log.error("Error calling Enabling system for approval", e);
            financeRequest.setStatus(RequestStatus.ERROR);
            financeRequest.setApprovalNotes("Error during approval: " + e.getMessage());
            financeRequestRepository.save(financeRequest);
        }
    }
    
    @Transactional
    public FinanceApprovalResponse declineRequest(String requestId, String declinedBy, String notes) {
        FinanceRequest financeRequest = financeRequestRepository.findByRequestId(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Finance request not found: " + requestId));
        
        if (financeRequest.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("Request " + requestId + " is not in PENDING status. Current status: " + financeRequest.getStatus());
        }
        
        // Publish decline decision to queue for processing
        ApprovalDecisionMessage decision = ApprovalDecisionMessage.builder()
                .requestId(requestId)
                .decision("DECLINED")
                .decidedBy(declinedBy)
                .notes(notes)
                .timestamp(LocalDateTime.now())
                .build();
        
        rabbitTemplate.convertAndSend(financeApprovalExchange, approvalDecisionRoutingKey, decision);
        log.info("Published decline decision for request {} to queue", requestId);
        
        return FinanceApprovalResponse.builder()
                .requestId(requestId)
                .status(RequestStatus.PENDING)
                .approvedAmount(null)
                .message("Decline decision queued for processing")
                .build();
    }
    
    @Transactional
    public void processDeclineDecision(String requestId, String declinedBy, String notes) {
        FinanceRequest financeRequest = financeRequestRepository.findByRequestId(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Finance request not found: " + requestId));
        
        if (financeRequest.getStatus() != RequestStatus.PENDING) {
            log.warn("Request {} is not in PENDING status. Current status: {}", requestId, financeRequest.getStatus());
            return;
        }
        
        // Update status to rejected
        financeRequest.setStatus(RequestStatus.REJECTED);
        financeRequest.setApprovalNotes("Manually declined by: " + declinedBy + ". " + (notes != null ? notes : "No reason provided"));
        financeRequestRepository.save(financeRequest);
        
        // Publish rejection event
        publishApprovalEvent(financeRequest, false, "Manually declined");
        
        log.info("Processed decline for finance request {} by {}", requestId, declinedBy);
    }
    
    public FinanceRequest getFinanceRequest(String requestId) {
        return financeRequestRepository.findByRequestId(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Finance request not found: " + requestId));
    }
    
    public List<FinanceRequest> getRequestsByCustomer(String customerId) {
        return financeRequestRepository.findByCustomerId(customerId);
    }
    
    public List<FinanceRequest> getRequestsByStatus(RequestStatus status) {
        return financeRequestRepository.findByStatus(status);
    }
    
    public List<FinanceRequest> getAllRequests() {
        return financeRequestRepository.findAll();
    }
    
    private void publishApprovalEvent(FinanceRequest request, boolean approved, String reason) {
        FinanceApprovalEvent event = FinanceApprovalEvent.builder()
                .requestId(request.getRequestId())
                .customerEmail(request.getCustomerId())
                .purchaseAmount(request.getAmount())
                .approvalCode(approved ? "APPROVED" : "REJECTED")
                .timestamp(LocalDateTime.now())
                .build();
        
        rabbitTemplate.convertAndSend(financeApprovalExchange, financeApprovalRoutingKey, event);
        log.info("Published finance approval event for request: {}, approved: {}", request.getRequestId(), approved);
    }
}
