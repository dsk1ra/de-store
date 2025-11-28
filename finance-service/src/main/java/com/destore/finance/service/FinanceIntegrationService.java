package com.destore.finance.service;

import com.destore.dto.ApprovalDecisionMessage;
import com.destore.dto.EnablingRequest;
import com.destore.dto.EnablingResponse;
import com.destore.dto.FinanceApprovalEvent;
import com.destore.dto.PendingApprovalMessage;
import com.destore.finance.client.ExternalFinanceClient;
import com.destore.finance.dto.FinanceApprovalRequest;
import com.destore.finance.dto.FinanceApprovalResponse;
import com.destore.finance.entity.FinanceRequest;
import com.destore.finance.entity.RequestStatus;
import com.destore.finance.repository.FinanceRequestRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinanceIntegrationService {

    private final FinanceRequestRepository financeRequestRepository;
    private final ExternalFinanceClient externalFinanceClient;
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

        log.info("Created finance request {} for customer {} with amount {}", requestId, request.getCustomerId(),
                request.getAmount());

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
            throw new IllegalStateException("Request " + requestId + " is not in PENDING status. Current status: "
                    + financeRequest.getStatus());
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
            // Check if the decision already came from the enabling simulator
            // (auto-processed from queue)
            if ("ENABLING_SIMULATOR".equals(approvedBy)) {
                // Decision was already made by the simulator via queue processing
                // Just apply the decision directly without calling the REST API again
                financeRequest.setStatus(RequestStatus.APPROVED);
                financeRequest.setExternalReferenceId(requestId);
                financeRequest.setApprovalNotes("Approved by: " + approvedBy + ". " + (notes != null ? notes : ""));
                financeRequestRepository.save(financeRequest);

                // Publish approval event
                publishApprovalEvent(financeRequest, true, notes != null ? notes : "Approved via queue processing");

                log.info("Processed approval for finance request {} by {}", requestId, approvedBy);
            } else {
                // Manual approval - need to call external finance system with circuit breaker
                EnablingRequest enablingRequest = EnablingRequest.builder()
                        .customerId(financeRequest.getCustomerId())
                        .amount(financeRequest.getAmount())
                        .requestId(requestId)
                        .build();

                EnablingResponse enablingResponse = callExternalFinanceService(enablingRequest);
                log.info("External finance service called for request {}", requestId);

                if (enablingResponse != null && enablingResponse.isApproved()) {
                    // Update status to approved
                    financeRequest.setStatus(RequestStatus.APPROVED);
                    financeRequest.setExternalReferenceId(enablingResponse.getRequestId());
                    financeRequest.setApprovalNotes("Approved by: " + approvedBy + ". " + (notes != null ? notes : "")
                            + ". Enabling: " + enablingResponse.getReason());
                    financeRequestRepository.save(financeRequest);

                    // Publish approval event
                    publishApprovalEvent(financeRequest, true, enablingResponse.getReason());

                    log.info("Processed approval for finance request {} by {}", requestId, approvedBy);
                } else {
                    // Enabling system rejected
                    financeRequest.setStatus(RequestStatus.REJECTED);
                    financeRequest.setApprovalNotes("Rejected by Enabling system: "
                            + (enablingResponse != null ? enablingResponse.getReason() : "Unknown reason"));
                    financeRequestRepository.save(financeRequest);

                    log.info("External finance service rejected request {}", requestId);
                }
            }
        } catch (Exception e) {
            log.error("Error calling external finance service for approval", e);
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
            throw new IllegalStateException("Request " + requestId + " is not in PENDING status. Current status: "
                    + financeRequest.getStatus());
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
        financeRequest.setApprovalNotes(
                "Manually declined by: " + declinedBy + ". " + (notes != null ? notes : "No reason provided"));
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

    /**
     * Republish all pending requests to the queue for processing by the enabling
     * simulator
     */
    @Transactional
    public int reprocessPendingRequests() {
        List<FinanceRequest> pendingRequests = financeRequestRepository.findByStatus(RequestStatus.PENDING);

        log.info("Found {} pending request(s) to reprocess", pendingRequests.size());

        for (FinanceRequest request : pendingRequests) {
            // Republish to pending approval queue
            PendingApprovalMessage message = PendingApprovalMessage.builder()
                    .requestId(request.getRequestId())
                    .customerId(request.getCustomerId())
                    .amount(request.getAmount())
                    .purpose(request.getApprovalNotes())
                    .timestamp(LocalDateTime.now())
                    .build();

            rabbitTemplate.convertAndSend(financeApprovalExchange, pendingApprovalRoutingKey, message);
            log.info("Republished pending request {} to queue for simulator processing", request.getRequestId());
        }

        return pendingRequests.size();
    }

    /**
     * Calls finance approval automation service with circuit breaker and retry protection
     */
    @CircuitBreaker(name = "finance-approval-automation", fallbackMethod = "externalFinanceFallback")
    @Retry(name = "finance-approval-automation")
    private EnablingResponse callExternalFinanceService(EnablingRequest request) {
        log.info("Calling external finance service for request: {}", request.getRequestId());
        return externalFinanceClient.approveRequest(request);
    }

    /**
     * Fallback method when external finance service is unavailable
     */
    private EnablingResponse externalFinanceFallback(EnablingRequest request, Exception ex) {
        log.error("Circuit breaker activated for external finance service. Request: {}, Error: {}",
                request.getRequestId(), ex.getMessage());

        return EnablingResponse.builder()
                .requestId(request.getRequestId())
                .approved(false)
                .reason("External finance service is temporarily unavailable. Request will be queued for processing.")
                .build();
    }

}
