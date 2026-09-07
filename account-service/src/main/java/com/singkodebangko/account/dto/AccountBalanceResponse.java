package com.singkodebangko.account.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Aligned DTO record for GET /accounts/{accountId}/balance response.
 */
public record AccountBalanceResponse(
        Long accountId,
        String accountNumber,
        Long userId,
        String currency,
        BigDecimal balance,
        String status,
        @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
        Instant updatedAt
) {}
