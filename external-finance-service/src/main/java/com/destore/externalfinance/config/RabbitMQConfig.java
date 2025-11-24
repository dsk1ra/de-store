package com.destore.externalfinance.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    
    @Value("${rabbitmq.exchange.finance-approval}")
    private String financeApprovalExchange;
    
    @Value("${rabbitmq.queue.pending-approval}")
    private String pendingApprovalQueue;
    
    @Value("${rabbitmq.queue.approval-decision}")
    private String approvalDecisionQueue;
    
    @Value("${rabbitmq.routing-key.pending-approval}")
    private String pendingApprovalRoutingKey;
    
    @Value("${rabbitmq.routing-key.approval-decision}")
    private String approvalDecisionRoutingKey;
    
    @Bean
    public TopicExchange financeApprovalExchange() {
        return new TopicExchange(financeApprovalExchange);
    }
    
    @Bean
    public Queue pendingApprovalQueue() {
        return new Queue(pendingApprovalQueue, true);
    }
    
    @Bean
    public Queue approvalDecisionQueue() {
        return new Queue(approvalDecisionQueue, true);
    }
    
    @Bean
    public Binding pendingApprovalBinding() {
        return BindingBuilder
                .bind(pendingApprovalQueue())
                .to(financeApprovalExchange())
                .with(pendingApprovalRoutingKey);
    }
    
    @Bean
    public Binding approvalDecisionBinding() {
        return BindingBuilder
                .bind(approvalDecisionQueue())
                .to(financeApprovalExchange())
                .with(approvalDecisionRoutingKey);
    }
    
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
