package com.singkodebangko.transaction.dto;

import java.math.BigDecimal;

/**
 * Response DTO record received from account-service debit/credit operations.
 */
public record DebitCreditResponse(
        String accountId,
        BigDecimal previousBalance,
        BigDecimal newBalance,
        String status
) {}
