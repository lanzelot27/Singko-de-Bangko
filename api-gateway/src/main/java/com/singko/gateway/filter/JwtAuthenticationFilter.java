package com.singko.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.singko.gateway.dto.ErrorResponse;
import com.singko.gateway.util.JwtValidator;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtValidator jwtValidator;
    private final ObjectMapper objectMapper;

    private static final List<String> PUBLIC_PATHS = List.of(
            "/login",
            "/eureka",
            "/actuator"
    );

    public JwtAuthenticationFilter(JwtValidator jwtValidator, ObjectMapper objectMapper) {
        this.jwtValidator = jwtValidator;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Allow public paths without authentication
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Missing or invalid authorization header");
        }

        String token = authHeader.substring(7).trim();
        try {
            JwtValidator.Claims claims = jwtValidator.validateAndExtract(token);

            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", claims.userId())
                    .header("X-User-Email", claims.email())
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (IllegalArgumentException ex) {
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Invalid or expired JWT token: " + ex.getMessage());
        }
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String path = exchange.getRequest().getURI().getPath();
        ErrorResponse errorResponse = ErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        );

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            return response.setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -100; // Execute early in filter chain
    }
}
