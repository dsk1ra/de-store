package com.destore.finance.controller;

import com.destore.dto.ApiResponse;
import com.destore.finance.dto.FinanceApprovalRequest;
import com.destore.finance.dto.FinanceApprovalResponse;
import com.destore.finance.entity.FinanceRequest;
import com.destore.finance.entity.RequestStatus;
import com.destore.finance.service.FinanceIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
@Slf4j
public class FinanceController {
    
    private final FinanceIntegrationService financeIntegrationService;
    
    @PostMapping("/approve")
    public ResponseEntity<ApiResponse<FinanceApprovalResponse>> requestApproval(@RequestBody FinanceApprovalRequest request) {
        try {
            FinanceApprovalResponse response = financeIntegrationService.requestApproval(request);
            return ResponseEntity.ok(new ApiResponse<>(true, "Finance request created and awaiting approval", response));
        } catch (Exception e) {
            log.error("Error processing finance request", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    @PutMapping("/approve/{requestId}")
    public ResponseEntity<ApiResponse<FinanceApprovalResponse>> approveRequest(
            @PathVariable String requestId,
            @RequestBody(required = false) java.util.Map<String, String> body) {
        try {
            String approvedBy = body != null ? body.getOrDefault("approvedBy", "Unknown") : "Unknown";
            String notes = body != null ? body.get("notes") : null;
            
            FinanceApprovalResponse response = financeIntegrationService.approveRequest(requestId, approvedBy, notes);
            return ResponseEntity.ok(new ApiResponse<>(true, "Finance request approved", response));
        } catch (IllegalArgumentException e) {
            log.error("Invalid request", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (IllegalStateException e) {
            log.error("Invalid state", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error approving finance request", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    @PutMapping("/decline/{requestId}")
    public ResponseEntity<ApiResponse<FinanceApprovalResponse>> declineRequest(
            @PathVariable String requestId,
            @RequestBody(required = false) java.util.Map<String, String> body) {
        try {
            String declinedBy = body != null ? body.getOrDefault("declinedBy", "Unknown") : "Unknown";
            String notes = body != null ? body.get("notes") : null;
            
            FinanceApprovalResponse response = financeIntegrationService.declineRequest(requestId, declinedBy, notes);
            return ResponseEntity.ok(new ApiResponse<>(true, "Finance request declined", response));
        } catch (IllegalArgumentException e) {
            log.error("Invalid request", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (IllegalStateException e) {
            log.error("Invalid state", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error declining finance request", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    @GetMapping("/request/{requestId}")
    public ResponseEntity<ApiResponse<FinanceRequest>> getFinanceRequest(@PathVariable String requestId) {
        try {
            FinanceRequest request = financeIntegrationService.getFinanceRequest(requestId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Finance request retrieved", request));
        } catch (Exception e) {
            log.error("Error retrieving finance request", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    @GetMapping("/status/{requestId}")
    public ResponseEntity<ApiResponse<FinanceRequest>> getRequestStatus(@PathVariable String requestId) {
        try {
            FinanceRequest request = financeIntegrationService.getFinanceRequest(requestId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Request status: " + request.getStatus(), request));
        } catch (Exception e) {
            log.error("Error retrieving request status", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    @GetMapping("/requests")
    public ResponseEntity<ApiResponse<List<FinanceRequest>>> getAllRequests(
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false, defaultValue = "100") int limit) {
        try {
            List<FinanceRequest> requests;
            if (status != null) {
                requests = financeIntegrationService.getRequestsByStatus(status);
            } else {
                requests = financeIntegrationService.getAllRequests();
            }
            
            // Apply limit
            if (requests.size() > limit) {
                requests = requests.subList(0, limit);
            }
            
            return ResponseEntity.ok(new ApiResponse<>(true, "Requests retrieved", requests));
        } catch (Exception e) {
            log.error("Error retrieving requests", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<FinanceRequest>>> getRequestsByCustomer(@PathVariable String customerId) {
        try {
            List<FinanceRequest> requests = financeIntegrationService.getRequestsByCustomer(customerId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Customer requests retrieved", requests));
        } catch (Exception e) {
            log.error("Error retrieving customer requests", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<FinanceRequest>>> getRequestsByStatus(@PathVariable RequestStatus status) {
        try {
            List<FinanceRequest> requests = financeIntegrationService.getRequestsByStatus(status);
            return ResponseEntity.ok(new ApiResponse<>(true, "Requests by status retrieved", requests));
        } catch (Exception e) {
            log.error("Error retrieving requests by status", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<FinanceRequest>>> getPendingRequests() {
        try {
            List<FinanceRequest> requests = financeIntegrationService.getRequestsByStatus(RequestStatus.PENDING);
            return ResponseEntity.ok(new ApiResponse<>(true, 
                    "Found " + requests.size() + " pending request(s) in queue", requests));
        } catch (Exception e) {
            log.error("Error retrieving pending requests", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    @GetMapping("/queue-stats")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getQueueStats() {
        try {
            List<FinanceRequest> pending = financeIntegrationService.getRequestsByStatus(RequestStatus.PENDING);
            List<FinanceRequest> approved = financeIntegrationService.getRequestsByStatus(RequestStatus.APPROVED);
            List<FinanceRequest> rejected = financeIntegrationService.getRequestsByStatus(RequestStatus.REJECTED);
            
            java.util.Map<String, Object> stats = new java.util.HashMap<>();
            stats.put("pendingInQueue", pending.size());
            stats.put("totalApproved", approved.size());
            stats.put("totalRejected", rejected.size());
            stats.put("totalProcessed", approved.size() + rejected.size());
            
            return ResponseEntity.ok(new ApiResponse<>(true, "Queue statistics retrieved", stats));
        } catch (Exception e) {
            log.error("Error retrieving queue statistics", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Finance service is running", "OK"));
    }
}
