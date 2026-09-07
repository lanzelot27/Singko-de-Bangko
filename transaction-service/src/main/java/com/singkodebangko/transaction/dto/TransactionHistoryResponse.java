package com.singkodebangko.transaction.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Aligned DTO record for GET /transactions/account/{accountId} response item.
 */
public record TransactionHistoryResponse(
        String transactionId,
        Long sourceAccountId,
        Long destinationAccountId,
        BigDecimal amount,
        String currency,
        String status,
        @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
        Instant timestamp
) {}
