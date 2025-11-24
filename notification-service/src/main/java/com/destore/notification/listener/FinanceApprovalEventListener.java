package com.destore.notification.listener;

import com.destore.dto.FinanceApprovalEvent;
import com.destore.notification.entity.NotificationType;
import com.destore.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FinanceApprovalEventListener {
    
    private final NotificationService notificationService;
    
    @RabbitListener(queues = "${rabbitmq.queue.finance-approval}")
    public void handleFinanceApprovalEvent(FinanceApprovalEvent event) {
        log.info("Received finance approval event for request: {}", event.getRequestId());
        
        String subject = "APPROVED".equals(event.getApprovalCode()) 
                ? "Finance Approval - APPROVED" 
                : "Finance Approval - REJECTED";
        
        String message = String.format(
                "Finance Request Update\n" +
                "Request ID: %s\n" +
                "Customer Email: %s\n" +
                "Amount: £%.2f\n" +
                "Approval Code: %s\n" +
                "Time: %s",
                event.getRequestId(),
                event.getCustomerEmail(),
                event.getPurchaseAmount(),
                event.getApprovalCode(),
                event.getTimestamp()
        );
        
        // Send email notification to customer
        notificationService.sendNotification(
                NotificationType.EMAIL,
                event.getCustomerEmail(),
                subject,
                message
        );
        
        // Send push notification
        notificationService.sendNotification(
                NotificationType.PUSH_NOTIFICATION,
                event.getCustomerEmail(),
                subject,
                message
        );
        
        log.info("Finance approval notifications sent for request: {}", event.getRequestId());
    }
}
