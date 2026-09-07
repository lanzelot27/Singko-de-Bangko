package com.singkodebangko.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Request DTO for PUT /accounts/{accountId}/adjust-balance.
 * Negative amount indicates deduction (DEBIT), positive indicates addition (CREDIT).
 */
public record AdjustBalanceRequest(
        @NotBlank(message = "Transaction reference is required")
        String transactionReference,

        @NotNull(message = "Amount is required")
        BigDecimal amount,

        @NotBlank(message = "Transaction type is required (DEBIT/CREDIT)")
        String type
) {}
