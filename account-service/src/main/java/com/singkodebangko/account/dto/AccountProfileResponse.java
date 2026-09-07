package com.singkodebangko.account.dto;

/**
 * Aligned DTO record for GET /accounts/{accountId}/profile response.
 */
public record AccountProfileResponse(
        Long accountId,
        String accountNumber,
        String accountType,
        String ownerName,
        String email,
        String status
) {}
