package com.singkodebangko.apigateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtil {

    @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String secret;

    private List<Key> getSigningKeys() {
        List<Key> keys = new ArrayList<>();

        // Candidate 1: Base64 decoded (standard Spring Boot JWT tutorial pattern)
        try {
            byte[] base64Bytes = Decoders.BASE64.decode(secret);
            keys.add(Keys.hmacShaKeyFor(base64Bytes));
        } catch (Exception ignored) {
        }

        // Candidate 2: Hex decoded (since 64 hex chars = 256 bits)
        if (secret.length() == 64 && secret.matches("^[0-9a-fA-F]+$")) {
            try {
                byte[] hexBytes = hexStringToByteArray(secret);
                keys.add(Keys.hmacShaKeyFor(hexBytes));
            } catch (Exception ignored) {
            }
        }

        // Candidate 3: Raw UTF-8 bytes
        try {
            byte[] rawBytes = secret.getBytes(StandardCharsets.UTF_8);
            if (rawBytes.length >= 32) {
                keys.add(Keys.hmacShaKeyFor(rawBytes));
            }
        } catch (Exception ignored) {
        }

        return keys;
    }

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public Claims extractAllClaims(String token) {
        List<Key> keys = getSigningKeys();
        for (Key key : keys) {
            try {
                return Jwts.parserBuilder()
                        .setSigningKey(key)
                        .build()
                        .parseClaimsJws(token)
                        .getBody();
            } catch (Exception ignored) {
                // Try next candidate key format
            }
        }
        return null;
    }

    public boolean isTokenExpired(Claims claims) {
        Date expiration = claims.getExpiration();
        return expiration != null && expiration.before(new Date());
    }

    public String extractUserId(Claims claims) {
        Object userId = claims.get("userId");
        if (userId != null) {
            return String.valueOf(userId);
        }
        return claims.getSubject();
    }

    public String extractEmail(Claims claims) {
        Object email = claims.get("email");
        if (email != null) {
            return String.valueOf(email);
        }
        return claims.getSubject();
    }
}
