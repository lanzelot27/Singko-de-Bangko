package com.singkodebangko.account.dto;

import java.math.BigDecimal;

/**
 * Immutable DTO record for debit/credit operations response.
 */
public record DebitCreditResponse(
        String accountId,
        BigDecimal previousBalance,
        BigDecimal newBalance,
        String status
) {}
