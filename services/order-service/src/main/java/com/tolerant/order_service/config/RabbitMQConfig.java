package com.tolerant.order_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "messaging.enabled", havingValue = "true")
public class RabbitMQConfig {

    private final RabbitTemplate rabbitTemplate;
    
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    public static final String NOTIFICATION_QUEUE = "notification.queue";
    public static final String NOTIFICATION_ROUTING_KEY = "notification.order";

    RabbitMQConfig(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Bean
    public Exchange notificationExchange() {
        return ExchangeBuilder
            .topicExchange(NOTIFICATION_EXCHANGE)
            .durable(true)
            .build();
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder
            .durable(NOTIFICATION_QUEUE)
            .build();
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, Exchange notificatioExchange) {
        return BindingBuilder
            .bind(notificationQueue)
            .to(notificatioExchange)
            .with(NOTIFICATION_ROUTING_KEY)
            .noargs();
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return template;
    }
}
