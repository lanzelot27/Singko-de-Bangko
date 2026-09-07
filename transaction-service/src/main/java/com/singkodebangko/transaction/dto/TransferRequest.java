package com.singkodebangko.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Aligned DTO record for POST /transactions/transfer request.
 */
public record TransferRequest(
        @NotNull(message = "Source account ID is required")
        Long sourceAccountId,

        @NotNull(message = "Destination account ID is required")
        Long destinationAccountId,

        @NotNull(message = "Transfer amount is required")
        @DecimalMin(value = "0.01", message = "Transfer amount must be greater than zero")
        BigDecimal amount,

        String currency,

        String description
) {
    public TransferRequest {
        if (currency == null || currency.isBlank()) {
            currency = "PHP";
        }
    }
}
