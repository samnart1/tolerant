package com.tolerant.api_gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        //skip auth for health checks and actuator endpoints
        String path = request.getPath().toString();
        if (path.contains("/actuator") || path.contains("/heatlh")) {
            return chain.filter(exchange);
        }

        // simple header validation. log and pass through
        String requestId = request.getHeaders().getFirst("X-Request-ID");
        if (requestId == null) {
            requestId = java.util.UUID.randomUUID().toString();
            ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-Request-ID", requestId)
                .build();

            exchange = exchange.mutate().request(mutatedRequest).build();
        }

        log.debug("Request ID: {} for path: {}", requestId, path);

        return chain.filter(exchange);

    }


    
}
