package com.tolerant.api_gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

@Configuration
public class GatewayConfig {
    
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
        // Order Service Routes
            .route("order-service-create", r -> r
                .path("/api/orders")
                .and()
                .method(HttpMethod.POST)
                .filters(f -> f
                    .circuitBreaker(c -> c
                        .setName("orderServiceCircuitBreaker")
                        .setFallbackUri("forward:/fallback/orders"))
                    .retry(config -> config
                        .setRetries(3)
                        .setMethods(HttpMethod.POST)))
                .uri("lb://order-service"))

            .route("order-service-get", r -> r
                .path("/api/orders/**")
                .and()
                .method(HttpMethod.GET)
                .filters(f -> f
                    .circuitBreaker(c -> c
                        .setName("orderServiceCircuitBreaker")
                        .setFallbackUri("forward:/fallback/orders")))
                .uri("lb://order-service"))

        // payment service routes
            .route("payment-service-process", r -> r
                .path("/api/payments/process")
                .and()
                .method(HttpMethod.POST)
                .filters(f -> f
                    .circuitBreaker(c -> c
                        .setName("paymentServiceCircuitBreaker")
                        .setFallbackUri("forward:/fallback/payments"))
                    .retry(config -> config
                        .setRetries(2)
                        .setMethods(HttpMethod.POST)))
                .uri("lb://payment-service"))

            .route("payment-service-get", r -> r
                .path("/api/payments/**")
                .and()
                .method(HttpMethod.GET)
                .filters(f -> f
                    .circuitBreaker(c -> c
                        .setName("paymentServiceCircuitBreaker")
                        .setFallbackUri("forward:/fallback/payments")))
                .uri("lb://payment-service"))

        // inventory service routes

            .route("inventory-service-reserve", r -> r
                .path("/api/inventory/reserve")
                .and()
                .method(HttpMethod.POST)
                .filters(f -> f
                    .circuitBreaker(c -> c
                        .setName("inventoryServiceCircuitBreaker")
                        .setFallbackUri("forward:/fallback/inventory"))
                    .retry(config -> config
                        .setRetries(2)
                        .setMethods(HttpMethod.POST)))
                .uri("lb://inventory-service"))

            .route("inventory-service-check", r -> r
                .path("/api/inventory/check")
                .and()
                .method(HttpMethod.POST)
                .filters(f -> f
                    .circuitBreaker(c -> c
                        .setName("inventoryServiceCircuitBreaker")
                        .setFallbackUri("forward:/fallback/inventory")))
                .uri("lb://inventory-service"))

            .route("inventory-service-get", r -> r
                .path("/api/inventory/**")
                .and()
                .method(HttpMethod.GET, HttpMethod.DELETE)
                .filters(f -> f
                    .circuitBreaker(c -> c
                        .setName("inventoryServiceCircuitBreaker")
                        .setFallbackUri("forward:/fallback/inventory")))
                .uri("lb://inventory-service"))

        // notification service routes
            .route("notification-service-send", r -> r
                .path("/api/notifications/send")
                .and()
                .method(HttpMethod.POST)
                .filters(f -> f
                    .circuitBreaker(c -> c
                        .setName("notificationServiceCircuitBreaker")
                        .setFallbackUri("forward:/fallback/notifications"))
                    .retry(config -> config
                        .setRetries(2)
                        .setMethods(HttpMethod.POST)))
                .uri("lb://notification-service"))

            .route("notification-service-get", r -> r
                .path("/api/notifications/**")
                .and()
                .method(HttpMethod.GET)
                .filters(f -> f
                    .circuitBreaker(c -> c
                        .setName("notificationServiceCircuitBreaker")
                        .setFallbackUri("forward:/fallback/notifications")))
                .uri("lb://notification-service"))

        // chaos routes
            .route("chaos-order", r -> r
                .path("/api/orders/chaos/**")
                .uri("lb://order-service"))

            .route("chaos-payment", r -> r
                .path("/api/payments/chaos/**")
                .uri("lb://payment-service"))

            .route("chaos-inventory", r -> r
                .path("/api/inventory/chaos/**")
                .uri("lb://inventory-service"))

            .route("chaos-notification", r -> r 
                .path("/api/notifications/chaos/**")
                .uri("lb://notification-service"))

            .build();
    }
}
