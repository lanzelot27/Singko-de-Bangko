package com.singko.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.singko.gateway.dto.ErrorResponse;
import com.singko.gateway.util.JwtValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ApiGatewayApplicationTests {

    @Autowired
    private JwtValidator jwtValidator;

    @Autowired
    private RouteLocator routeLocator;

    private final String testSecret = "singko-de-bangko-super-secure-jwt-secret-key-2026";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("TC-GW-02: Dynamic Routing via Gateway - Verify configured downstream routes")
    void testDynamicRoutingViaGateway() {
        assertNotNull(routeLocator, "RouteLocator should be present in context");
        List<Route> routes = routeLocator.getRoutes().collectList().block();
        assertNotNull(routes, "Route list should not be null");

        // Verify downstream routes exist for Auth, Account, and Transaction services
        boolean hasAuth = routes.stream().anyMatch(r -> r.getId().equals("auth-service"));
        boolean hasAccount = routes.stream().anyMatch(r -> r.getId().equals("account-service"));
        boolean hasTransaction = routes.stream().anyMatch(r -> r.getId().equals("transaction-service"));

        assertTrue(hasAuth, "Route for auth-service must be defined");
        assertTrue(hasAccount, "Route for account-service must be defined");
        assertTrue(hasTransaction, "Route for transaction-service must be defined");
    }

    @Test
    @DisplayName("TC-GW-03: Service Unreachable Handling / Standard Error Contract verification")
    void testServiceUnreachableAndErrorHandling() {
        // Test standard unified error response model required by contracts
        ErrorResponse errorResponse = ErrorResponse.of(
                503,
                "Service Unavailable",
                "Service temporarily unreachable",
                "/login"
        );

        assertNotNull(errorResponse.timestamp(), "Timestamp must be generated in ISO format");
        assertEquals(503, errorResponse.status(), "Status code should match 503");
        assertEquals("Service Unavailable", errorResponse.error(), "Error reason phrase should match");
        assertEquals("Service temporarily unreachable", errorResponse.message(), "Message should match");
        assertEquals("/login", errorResponse.path(), "Path should match");
    }

    @Test
    @DisplayName("Unit Test: Valid JWT extraction of userId and email claims")
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
    @DisplayName("Unit Test: Rejection of tampered JWT token signature")
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
    @DisplayName("Unit Test: Rejection of expired JWT token")
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
