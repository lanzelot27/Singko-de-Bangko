package com.singkodebangko.transaction.dto;

import java.math.BigDecimal;

/**
 * Request DTO record sent to account-service for crediting funds.
 */
public record CreditRequest(
        BigDecimal amount
) {}
