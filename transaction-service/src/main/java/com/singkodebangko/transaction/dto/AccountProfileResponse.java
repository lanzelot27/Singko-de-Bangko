package com.singkodebangko.transaction.dto;

public record AccountProfileResponse(
        Long accountId,
        String accountNumber,
        String accountType,
        String ownerName,
        String email,
        String status
) {}
