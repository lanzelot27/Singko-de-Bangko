package com.singkodebangko.transaction.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Aligned DTO record for POST /transactions/transfer response (HTTP 201 Created).
 */
public record TransferResponse(
        String transactionId,
        Long sourceAccountId,
        Long destinationAccountId,
        BigDecimal amount,
        String currency,
        String status,
        String description,
        @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
        Instant timestamp
) {}
