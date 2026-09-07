package com.singkodebangko.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Immutable DTO record for credit request.
 */
public record CreditRequest(
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be strictly greater than 0")
        BigDecimal amount
) {}
