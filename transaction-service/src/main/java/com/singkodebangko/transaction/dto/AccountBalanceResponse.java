package com.singkodebangko.transaction.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.Instant;

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
