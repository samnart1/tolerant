package com.tolerant.api_gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class MetricsFilter implements GlobalFilter, Ordered {
    
    private final MeterRegistry meterRegistry;

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().toString();
        String method = request.getMethod().toString();

        Timer.Sample sample = Timer.start(meterRegistry);

        // increment request timer
        Counter.builder("gateway.requests.total")
            .tag("method", method)
            .tag("path", extractServiceName(path))
            .register(meterRegistry)
            .increment();

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            String statusCode = exchange.getResponse().getStatusCode() != null
                ? exchange.getResponse().getStatusCode().toString()
                : "UNKNOWN";
                
            sample.stop(Timer.builder("gateway.requests.duration")
                .tag("method", method)
                .tag("path", extractServiceName(path))
                .tag("status", statusCode)
                .register(meterRegistry));
        }));
    }

    private String extractServiceName(String path) {
        if (path.startsWith("/api/orders")) return "order-service";
        if (path.startsWith("/api/payments")) return "payment-service";
        if (path.startsWith("/api/inventory")) return "inventory-service";
        if (path.startsWith("/api/notifications")) return "notification-service";
        return "unknown";
    }

}
