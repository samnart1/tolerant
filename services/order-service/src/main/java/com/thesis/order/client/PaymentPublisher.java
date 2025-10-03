package com.thesis.order.client;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesis.order.model.Order;
import com.thesis.order.model.PaymentRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentPublisher {
    
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${rabbitmq.exchange.payment:payment.exchange}")
    private String paymentExchange;

    @Value("${rabbitmq.routing-key.payment:payment.process}")
    private String paymentRoutingKey;

    public void publishPaymentRequest(Order order) {
        try {
            PaymentRequest paymentRequest = PaymentRequest.builder()
                .orderId(order.getId())
                .productId(order.getProductId())
                .quantity(order.getQuantity())
                .paymentMethod(order.getPaymentMethod())
                .amount(calculateAmount(order))
                .build();

            String message = objectMapper.writeValueAsString(paymentRequest);

            rabbitTemplate.convertAndSend(paymentExchange, paymentRoutingKey, message);

            log.info("Published payment request for order {}: {}", order.getId(), message);

        } catch (Exception e) {
            log.error("Failed to publish payment request for order {}: {}", e, order.getId(), e.getMessage());

            throw new RuntimeException("Failed to publish payment request", e);
        }
    }

    private Double calculateAmount(Order order) {
        return order.getQuantity() * 10.0; //10euro for 1
    }
}
