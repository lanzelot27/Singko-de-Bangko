package com.singko.gateway.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.singko.gateway.dto.ErrorResponse;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;

/**
 * Handles unreachable downstream microservices (TC-GW-03) and unhandled gateway exceptions,
 * returning the unified JSON error response compliant with contracts.md.
 */
@Component
@Order(-2) // Execute before default Spring WebFlux error handlers
public class GlobalErrorExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    public GlobalErrorExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "Internal Server Error";

        if (ex instanceof ResponseStatusException rse) {
            HttpStatus resolved = HttpStatus.resolve(rse.getStatusCode().value());
            status = resolved != null ? resolved : HttpStatus.INTERNAL_SERVER_ERROR;
            message = rse.getReason() != null ? rse.getReason() : ex.getMessage();
        } else if (isConnectionRefused(ex)) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = "Downstream service is currently unreachable";
        } else if (ex.getMessage() != null && !ex.getMessage().isBlank()) {
            message = ex.getMessage();
        }

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

    private boolean isConnectionRefused(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof ConnectException) {
                return true;
            }
            if (current.getMessage() != null && (
                    current.getMessage().contains("Connection refused") ||
                    current.getMessage().contains("Connection reset") ||
                    current.getMessage().contains("failed to resolve")
            )) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}