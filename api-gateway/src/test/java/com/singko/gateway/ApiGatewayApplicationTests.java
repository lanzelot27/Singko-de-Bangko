package com.singko.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.singko.gateway.util.JwtValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ApiGatewayApplicationTests {

    @Autowired
    private JwtValidator jwtValidator;

    private final String testSecret = "singko-de-bangko-super-secure-jwt-secret-key-2026";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Unit Test 1: Gateway Spring context loads successfully")
    void testContextLoads() {
        assertNotNull(jwtValidator, "JwtValidator bean should be loaded into context");
    }

    @Test
    @DisplayName("Unit Test 2: Valid JWT extraction of userId and email claims")
    void testJwtValidationSuccess() throws Exception {
        long now = Instant.now().getEpochSecond();
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(objectMapper.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT")));
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(objectMapper.writeValueAsBytes(Map.of("sub", "1001", "email", "juan.delacruz@neobank.com", "exp", now + 3600)));

        String data = header + "." + payload;
        String signature = sign(data, testSecret);
        String validToken = data + "." + signature;

        JwtValidator.Claims claims = jwtValidator.validateAndExtract(validToken);
        assertEquals("1001", claims.userId(), "Extracted userId should match '1001'");
        assertEquals("juan.delacruz@neobank.com", claims.email(), "Extracted email should match");
    }

    @Test
    @DisplayName("Unit Test 3: Rejection of tampered JWT token signature")
    void testJwtValidationTamperedSignature() throws Exception {
        long now = Instant.now().getEpochSecond();
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(objectMapper.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT")));
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(objectMapper.writeValueAsBytes(Map.of("sub", "1001", "email", "attacker@neobank.com", "exp", now + 3600)));

        String tamperedToken = header + "." + payload + ".invalidSignatureString12345";

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                jwtValidator.validateAndExtract(tamperedToken));
        assertTrue(ex.getMessage().contains("Invalid token signature") || ex.getMessage().contains("failed"),
                "Exception should indicate signature failure");
    }

    @Test
    @DisplayName("Unit Test 4: Rejection of expired JWT token")
    void testJwtValidationExpiredToken() throws Exception {
        long pastTime = Instant.now().getEpochSecond() - 3600;
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(objectMapper.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT")));
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(objectMapper.writeValueAsBytes(Map.of("sub", "1001", "email", "expired@neobank.com", "exp", pastTime)));

        String data = header + "." + payload;
        String signature = sign(data, testSecret);
        String expiredToken = data + "." + signature;

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                jwtValidator.validateAndExtract(expiredToken));
        assertTrue(ex.getMessage().contains("expired"), "Exception should indicate token has expired");
    }

    private String sign(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }
}
