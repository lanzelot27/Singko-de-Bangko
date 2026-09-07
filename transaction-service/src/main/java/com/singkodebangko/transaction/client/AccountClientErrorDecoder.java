package com.singkodebangko.transaction.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.singkodebangko.transaction.exception.AccountNotFoundException;
import com.singkodebangko.transaction.exception.InsufficientBalanceException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
public class AccountClientErrorDecoder implements ErrorDecoder {

    private static final Logger log = LoggerFactory.getLogger(AccountClientErrorDecoder.class);
    private final ErrorDecoder defaultDecoder = new Default();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Exception decode(String methodKey, Response response) {
        String errorMessage = extractErrorMessage(response);

        log.warn("Feign call failed [{}]: HTTP {} - {}", methodKey, response.status(), errorMessage);

        if (response.status() == 404) {
            return new AccountNotFoundException(
                    errorMessage != null ? errorMessage : "Account not found in account-service"
            );
        }

        if (response.status() == 400) {
            return new InsufficientBalanceException(
                    errorMessage != null ? errorMessage : "Debit failed: Insufficient balance or invalid amount"
            );
        }

        return defaultDecoder.decode(methodKey, response);
    }

    private String extractErrorMessage(Response response) {
        if (response.body() == null) {
            return null;
        }
        try (InputStream is = response.body().asInputStream()) {
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            if (content.isBlank()) {
                return null;
            }
            JsonNode node = objectMapper.readTree(content);
            if (node.has("message")) {
                return node.get("message").asText();
            }
            return content;
        } catch (Exception e) {
            log.debug("Could not parse Feign error body", e);
            return null;
        }
    }
}
