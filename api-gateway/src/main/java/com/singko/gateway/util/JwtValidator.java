package com.singko.gateway.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

@Component
public class JwtValidator {

    private final String secret;
    private final ObjectMapper objectMapper;

    public record Claims(String userId, String email) {}

    public JwtValidator(
            @Value("${jwt.secret:singko-de-bangko-super-secure-jwt-secret-key-2026}") String secret,
            ObjectMapper objectMapper) {
        this.secret = secret;
        this.objectMapper = objectMapper;
    }

    public Claims validateAndExtract(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token is empty");
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid token format");
        }

        String dataToSign = parts[0] + "." + parts[1];
        String expectedSignature = sign(dataToSign, secret);

        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                parts[2].getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("Invalid token signature");
        }

        try {
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode payload = objectMapper.readTree(payloadBytes);

            if (payload.has("exp")) {
                long exp = payload.get("exp").asLong();
                if (Instant.now().getEpochSecond() > exp) {
                    throw new IllegalArgumentException("Token has expired");
                }
            }

            String sub = payload.has("sub") ? payload.get("sub").asText() : "";
            String email = payload.has("email") ? payload.get("email").asText() : "";

            return new Claims(sub, email);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to decode token payload: " + e.getMessage());
        }
    }

    private static String sign(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Signing failed", e);
        }
    }
}
