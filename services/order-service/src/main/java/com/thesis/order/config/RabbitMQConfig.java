package com.thesis.order.config;

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
    
    @Value("${rabbitmq.exchange.payment}")
    private String paymentExchange;

    @Value("${rabbitmq.queue.payment}")
    private String paymentQueue;

    @Value("${rabbitmq.routing-key.payment}")
    private String paymentRoutingKey;

    @Bean
    public DirectExchange paymentExchange() {
        return new DirectExchange(paymentExchange);
    }

    @Bean
    public Queue paymentQueue() {
        return QueueBuilder.durable(paymentQueue)
            .withArgument("x-dead-letter-exchange", "dlx.exchange")
            .withArgument("x-message-ttl", 300000)
            .build();
    }

    @Bean
    public Binding paymentBinding(Queue paymentQuue, DirectExchange paymentExchange) {
        return BindingBuilder.bind(paymentQuue)
            .to(paymentExchange)
            .with(paymentRoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
