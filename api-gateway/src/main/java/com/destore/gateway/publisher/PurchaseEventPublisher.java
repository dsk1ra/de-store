package com.destore.gateway.publisher;

import com.destore.gateway.event.PurchaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PurchaseEventPublisher {
    
    private final RabbitTemplate rabbitTemplate;
    
    @Value("${rabbitmq.exchange.purchase:purchase.exchange}")
    private String purchaseExchange;
    
    @Value("${rabbitmq.routing-key.purchase:purchase.tracking}")
    private String purchaseRoutingKey;
    
    public void publishPurchaseEvent(PurchaseEvent event) {
        try {
            // Send the event object directly - RabbitTemplate's Jackson2JsonMessageConverter
            // will handle serialization and add proper type headers
            rabbitTemplate.convertAndSend(purchaseExchange, purchaseRoutingKey, event);
            log.info("Published purchase event for order: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Error publishing purchase event: {}", e.getMessage(), e);
        }
    }
}
