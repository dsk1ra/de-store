package com.destore.notification.listener;

import com.destore.dto.LowStockEvent;
import com.destore.notification.entity.NotificationType;
import com.destore.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LowStockEventListener {
    
    private final NotificationService notificationService;
    
    @RabbitListener(queues = "${rabbitmq.queue.low-stock}")
    public void handleLowStockEvent(LowStockEvent event) {
        log.info("Received low stock event for product: {}", event.getProductCode());
        
        String subject = String.format("Low Stock Alert - Product: %s", event.getProductCode());
        String message = String.format(
                "Product %s is running low on stock.\n" +
                "Current Stock: %d\n" +
                "Threshold: %d\n" +
                "Store ID: %s\n" +
                "Time: %s",
                event.getProductCode(),
                event.getCurrentQuantity(),
                event.getThreshold(),
                event.getStoreId(),
                event.getTimestamp()
        );
        
        // Send email notification to store manager
        notificationService.sendNotification(
                NotificationType.EMAIL,
                "store-manager@destore.com",
                subject,
                message
        );
        
        // Send SMS notification
        notificationService.sendNotification(
                NotificationType.SMS,
                "+1234567890",
                subject,
                message
        );
        
        log.info("Low stock notifications sent for product: {}", event.getProductCode());
    }
}
