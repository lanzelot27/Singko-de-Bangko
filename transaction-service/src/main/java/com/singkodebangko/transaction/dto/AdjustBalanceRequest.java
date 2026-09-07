package com.singkodebangko.transaction.dto;

import java.math.BigDecimal;

/**
 * Request DTO sent to account-service PUT /accounts/{accountId}/adjust-balance.
 */
public record AdjustBalanceRequest(
        String transactionReference,
        BigDecimal amount,
        String type
) {}
