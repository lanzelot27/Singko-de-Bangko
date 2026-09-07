package com.singkodebangko.transaction.security;

public record JwtClaims(
        Long userId,
        String email
) {
}
