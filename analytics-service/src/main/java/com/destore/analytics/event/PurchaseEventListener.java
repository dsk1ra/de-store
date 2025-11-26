package com.destore.analytics.event;

import com.destore.analytics.dto.TransactionRequest;
import com.destore.analytics.service.AnalyticsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PurchaseEventListener {

    private final AnalyticsService analyticsService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "${rabbitmq.queue.purchase}")
    public void handlePurchaseEvent(String message) {
        log.info("Received purchase event: {}", message);
        try {
            TransactionRequest transaction = objectMapper.readValue(message, TransactionRequest.class);
            analyticsService.trackTransaction(transaction);
            log.info("Successfully processed purchase event for transaction: {}", transaction.getTransactionId());
        } catch (Exception e) {
            log.error("Error processing purchase event: {}", e.getMessage(), e);
        }
    }
}
