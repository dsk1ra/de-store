package com.destore.analytics.event;

import com.destore.analytics.dto.TransactionRequest;
import com.destore.analytics.service.AnalyticsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PurchaseEventListener {

    private final AnalyticsService analyticsService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "${rabbitmq.queue.purchase}")
    public void handlePurchaseEvent(Message message) {
        try {
            String json = new String(message.getBody());
            log.info("Received purchase event: {}", json);
            
            TransactionRequest transaction = objectMapper.readValue(json, TransactionRequest.class);
            log.info("Parsed purchase event for order: {}", transaction.getOrderId());
            
            analyticsService.trackTransaction(transaction);
            log.info("Successfully processed purchase event for order: {}", transaction.getOrderId());
        } catch (Exception e) {
            log.error("Error processing purchase event: {}", e.getMessage(), e);
        }
    }
}
