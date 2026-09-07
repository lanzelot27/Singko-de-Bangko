// package com.neobank.gateway.security;

// import io.jsonwebtoken.Claims;
// import io.jsonwebtoken.JwtException;
// import io.jsonwebtoken.Jwts;
// import io.jsonwebtoken.security.Keys;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.cloud.gateway.filter.GatewayFilterChain;
// import org.springframework.cloud.gateway.filter.GlobalFilter;
// import org.springframework.core.Ordered;
// import org.springframework.core.io.buffer.DataBuffer;
// import org.springframework.http.HttpHeaders;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.MediaType;
// import org.springframework.http.server.reactive.ServerHttpRequest;
// import org.springframework.http.server.reactive.ServerHttpResponse;
// import org.springframework.stereotype.Component;
// import org.springframework.web.server.ServerWebExchange;
// import reactor.core.publisher.Mono;

// import javax.crypto.SecretKey;
// import java.nio.charset.StandardCharsets;

// @Component
// public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

//     @Value("${jwt.secret}")
//     private String secretKey;

//     @Override
//     public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//         ServerHttpRequest request = exchange.getRequest();

//         // Allow public paths like /login without token validation
//         if (request.getPath().value().equals("/login")) {
//             return chain.filter(exchange);
//         }

//         if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
//             return onError(exchange, "Missing Authorization Header", HttpStatus.UNAUTHORIZED);
//         }

//         String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
//         if (authHeader == null || !authHeader.startsWith("Bearer ")) {
//             return onError(exchange, "Invalid Authorization Header Format", HttpStatus.UNAUTHORIZED);
//         }

//         String token = authHeader.substring(7);

//         try {
//             SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
//             Claims claims = Jwts.parser()
//                     .verifyWith(key)
//                     .build()
//                     .parseSignedClaims(token)
//                     .getPayload();

//             String userId = claims.getSubject();
//             String email = claims.get("email", String.class);

//             // Mutate request to inject headers for downstream services
//             ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
//                     .header("X-User-Id", userId)
//                     .header("X-User-Email", email)
//                     .build();

//             return chain.filter(exchange.mutate().request(mutatedRequest).build());

//         } catch (JwtException | IllegalArgumentException e) {
//             return onError(exchange, "Invalid or expired JWT token", HttpStatus.UNAUTHORIZED);
//         }
//     }

//     private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
//         ServerHttpResponse response = exchange.getResponse();
//         response.setStatusCode(status);
//         response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

//         String errorBody = String.format(
//                 "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"Unauthorized\",\"message\":\"%s\",\"path\":\"%s\"}",
//                 java.time.Instant.now().toString(), status.value(), message, exchange.getRequest().getPath().value()
//         );

//         DataBuffer buffer = response.bufferFactory().wrap(errorBody.getBytes(StandardCharsets.UTF_8));
//         return response.writeWith(Mono.just(buffer));
//     }

//     @Override
//     public int getOrder() {
//         return -100;
//     }
// }