package com.destore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Message DTO for finance approval decisions.
 * Used for communication between finance-approval-automation and finance-service via RabbitMQ.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalDecisionMessage {
    private String requestId;
    private String decision; // APPROVED or DECLINED
    private String decidedBy;
    private String notes;
    private LocalDateTime timestamp;
}
