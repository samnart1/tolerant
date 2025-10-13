package com.tolerant.order_service.client;

import java.time.Duration;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.tolerant.order_service.model.PaymentRequest;
import com.tolerant.order_service.model.PaymentResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentClient {

    private final WebClient.Builder webClientBuilder;

    public Mono<PaymentResponse> processPayment(PaymentRequest request, Duration timeout) {
        log.info("Calling Payment service for order: {}", request.getOrderId());

        return webClientBuilder.build()
            .post()
            .uri("http://payment-service/api/payments/process")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(PaymentResponse.class)
            .timeout(timeout)
            .doOnSuccess(response -> log.info("Payment", timeout));
    }

    public PaymentResponse processPaymentSync(PaymentRequest request, Duration timeout) {
        return processPayment(request, timeout).block();
    }

}
