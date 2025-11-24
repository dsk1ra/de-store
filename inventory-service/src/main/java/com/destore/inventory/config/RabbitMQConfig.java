package com.destore.inventory.config;

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
    
    @Value("${rabbitmq.exchange.low-stock}")
    private String lowStockExchange;
    
    @Value("${rabbitmq.queue.low-stock}")
    private String lowStockQueue;
    
    @Value("${rabbitmq.routing-key.low-stock}")
    private String lowStockRoutingKey;
    
    @Bean
    public TopicExchange lowStockExchange() {
        return new TopicExchange(lowStockExchange);
    }
    
    @Bean
    public Queue lowStockQueue() {
        return new Queue(lowStockQueue, true);
    }
    
    @Bean
    public Binding lowStockBinding() {
        return BindingBuilder.bind(lowStockQueue())
                .to(lowStockExchange())
                .with(lowStockRoutingKey);
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
