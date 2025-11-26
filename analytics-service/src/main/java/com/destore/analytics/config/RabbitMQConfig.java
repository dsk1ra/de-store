package com.destore.analytics.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.purchase}")
    private String purchaseExchange;

    @Value("${rabbitmq.queue.purchase}")
    private String purchaseQueue;

    @Value("${rabbitmq.routing-key.purchase}")
    private String purchaseRoutingKey;

    @Bean
    public Queue purchaseQueue() {
        return new Queue(purchaseQueue, true);
    }

    @Bean
    public TopicExchange purchaseExchange() {
        return new TopicExchange(purchaseExchange);
    }

    @Bean
    public Binding purchaseBinding() {
        return BindingBuilder
                .bind(purchaseQueue())
                .to(purchaseExchange())
                .with(purchaseRoutingKey);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
}
