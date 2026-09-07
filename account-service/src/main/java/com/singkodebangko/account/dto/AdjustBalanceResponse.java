package com.singkodebangko.account.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO for PUT /accounts/{accountId}/adjust-balance.
 */
public record AdjustBalanceResponse(
        Long accountId,
        BigDecimal newBalance,
        @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
        Instant updatedAt
) {}
