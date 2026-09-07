package com.singkodebangko.apigateway.filter;

import com.singkodebangko.apigateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    // Public endpoints per contracts.md (including /auth fallback for backward compatibility)
    private static final List<String> OPEN_ENDPOINTS = List.of(
            "/login",
            "/auth",
            "/actuator"
    );

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. Allow CORS preflight requests without authentication
        if (request.getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        // 2. Allow public endpoints to pass through without JWT (stripping spoofed headers)
        if (isOpenEndpoint(path)) {
            ServerHttpRequest cleanRequest = request.mutate()
                    .headers(headers -> {
                        headers.remove("X-User-Id");
                        headers.remove("X-User-Email");
                    })
                    .build();
            return chain.filter(exchange.mutate().request(cleanRequest).build());
        }

        // 3. Validate Authorization header on secured routes (/accounts/**, /transactions/**)
        List<String> authHeaders = request.getHeaders().get(HttpHeaders.AUTHORIZATION);
        if (authHeaders == null || authHeaders.isEmpty()) {
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Missing authorization token", path);
        }

        String authHeader = authHeaders.get(0);
        if (!authHeader.startsWith("Bearer ")) {
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Authorization header must start with Bearer", path);
        }

        String token = authHeader.substring(7).trim();
        Claims claims = jwtUtil.extractAllClaims(token);
        if (claims == null) {
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Invalid authorization token", path);
        }

        if (jwtUtil.isTokenExpired(claims)) {
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Authorization token has expired", path);
        }

        // 4. Extract verified user context from JWT claims per contracts.md Section 1.1
        String userId = jwtUtil.extractUserId(claims);
        String userEmail = jwtUtil.extractEmail(claims);

        // 5. Inject validated headers downstream using .set() to guarantee overwrite
        ServerHttpRequest mutatedRequest = request.mutate()
                .headers(headers -> {
                    headers.set("X-User-Id", userId != null ? userId : "");
                    headers.set("X-User-Email", userEmail != null ? userEmail : "");
                })
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean isOpenEndpoint(String path) {
        for (String endpoint : OPEN_ENDPOINTS) {
            if (path.equals(endpoint) || path.startsWith(endpoint + "/")) {
                return true;
            }
        }
        return false;
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status, String message, String path) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        String errorJson = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                timestamp,
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        );

        byte[] bytes = errorJson.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1; // Highest precedence before routing
    }
}
