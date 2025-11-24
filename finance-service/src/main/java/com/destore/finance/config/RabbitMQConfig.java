package com.destore.finance.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
    
    @Value("${rabbitmq.queue.finance-approval}")
    private String financeApprovalQueue;
    
    @Value("${rabbitmq.routing-key.finance-approval}")
    private String financeApprovalRoutingKey;
    
    @Value("${rabbitmq.queue.pending-approval}")
    private String pendingApprovalQueue;
    
    @Value("${rabbitmq.routing-key.pending-approval}")
    private String pendingApprovalRoutingKey;
    
    @Value("${rabbitmq.queue.approval-decision}")
    private String approvalDecisionQueue;
    
    @Value("${rabbitmq.routing-key.approval-decision}")
    private String approvalDecisionRoutingKey;
    
    @Bean
    public TopicExchange financeApprovalExchange() {
        return new TopicExchange(financeApprovalExchange);
    }
    
    @Bean
    public Queue financeApprovalQueue() {
        return new Queue(financeApprovalQueue, true);
    }
    
    @Bean
    public Binding financeApprovalBinding() {
        return BindingBuilder.bind(financeApprovalQueue())
                .to(financeApprovalExchange())
                .with(financeApprovalRoutingKey);
    }
    
    @Bean
    public Queue pendingApprovalQueue() {
        return new Queue(pendingApprovalQueue, true);
    }
    
    @Bean
    public Binding pendingApprovalBinding() {
        return BindingBuilder.bind(pendingApprovalQueue())
                .to(financeApprovalExchange())
                .with(pendingApprovalRoutingKey);
    }
    
    @Bean
    public Queue approvalDecisionQueue() {
        return new Queue(approvalDecisionQueue, true);
    }
    
    @Bean
    public Binding approvalDecisionBinding() {
        return BindingBuilder.bind(approvalDecisionQueue())
                .to(financeApprovalExchange())
                .with(approvalDecisionRoutingKey);
    }
    
    @Bean
    public MessageConverter jackson2MessageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(mapper);
    }
    
    @Bean
    @SuppressWarnings("null")
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jackson2MessageConverter());
        return template;
    }
}
