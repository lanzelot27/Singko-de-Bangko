package com.singkodebangko.transaction.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.Instant;

public record AdjustBalanceResponse(
        Long accountId,
        BigDecimal newBalance,
        @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
        Instant updatedAt
) {}
