package com.singkodebangko.account.security;

/**
 * Validated JWT Claims representation.
 */
public record JwtClaims(
        Long userId,
        String email
) {
}
