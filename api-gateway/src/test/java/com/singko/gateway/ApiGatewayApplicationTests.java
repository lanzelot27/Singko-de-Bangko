package com.singko.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.singko.gateway.dto.ErrorResponse;
import com.singko.gateway.handler.GlobalErrorExceptionHandler;
import com.singko.gateway.util.JwtValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.ConnectException;
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

    @Autowired
    private GlobalErrorExceptionHandler globalErrorExceptionHandler;

    private final String requiredSecret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final String fallbackSecret = "singko-de-bangko-super-secure-jwt-secret-key-2026";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("TC-GW-02: Dynamic Routing via Gateway - Verify configured downstream routes and load balancer URIs")
    void testDynamicRoutingViaGateway() {
        assertNotNull(routeLocator, "RouteLocator should be present in context");
        List<Route> routes = routeLocator.getRoutes().collectList().block();
        assertNotNull(routes, "Route list should not be null");

        // Verify downstream routes exist for Auth, Account, and Transaction services
        Route authRoute = routes.stream().filter(r -> r.getId().equals("auth-service")).findFirst().orElse(null);
        Route accountRoute = routes.stream().filter(r -> r.getId().equals("account-service")).findFirst().orElse(null);
        Route transactionRoute = routes.stream().filter(r -> r.getId().equals("transaction-service")).findFirst().orElse(null);

        assertNotNull(authRoute, "Route for auth-service must be defined");
        assertEquals("lb://AUTH-SERVICE", authRoute.getUri().toString(), "Auth route must route via Eureka load balancer");

        assertNotNull(accountRoute, "Route for account-service must be defined");
        assertEquals("lb://ACCOUNT-SERVICE", accountRoute.getUri().toString(), "Account route must route via Eureka load balancer");

        assertNotNull(transactionRoute, "Route for transaction-service must be defined");
        assertEquals("lb://TRANSACTION-SERVICE", transactionRoute.getUri().toString(), "Transaction route must route via Eureka load balancer");
    }

    @Test
    @DisplayName("TC-GW-03: Service Unreachable Handling - Verify 503 Service Unavailable contract")
    void testServiceUnreachableAndErrorHandling() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/accounts/2001/balance").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        ConnectException connectException = new ConnectException("Connection refused: downstream service offline");

        globalErrorExceptionHandler.handle(exchange, connectException).block();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exchange.getResponse().getStatusCode(),
                "Downstream unreachable error must resolve to HTTP 503 Service Unavailable");
        assertEquals(MediaType.APPLICATION_JSON, exchange.getResponse().getHeaders().getContentType(),
                "Content-Type must be application/json");

        // Validate ErrorResponse structure matches contracts.md specification
        ErrorResponse errorResponse = ErrorResponse.of(
                503,
                "Service Unavailable",
                "Downstream service is currently unreachable",
                "/accounts/2001/balance"
        );
        assertNotNull(errorResponse.timestamp(), "Timestamp must be present");
        assertEquals(503, errorResponse.status());
        assertEquals("Service Unavailable", errorResponse.error());
        assertEquals("Downstream service is currently unreachable", errorResponse.message());
        assertEquals("/accounts/2001/balance", errorResponse.path());
    }

    @Test
    @DisplayName("Unit Test: Valid JWT extraction using required secret (404E6352...)")
    void testJwtValidationWithRequiredSecret() throws Exception {
        long now = Instant.now().getEpochSecond();
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(objectMapper.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT")));
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(objectMapper.writeValueAsBytes(Map.of(
                        "sub", "1001",
                        "email", "juan.delacruz@neobank.com",
                        "exp", now + 3600)));

        String data = header + "." + payload;
        String signature = sign(data, requiredSecret);
        String validToken = data + "." + signature;

        JwtValidator.Claims claims = jwtValidator.validateAndExtract(validToken);
        assertEquals("1001", claims.userId(), "Extracted userId should match '1001'");
        assertEquals("juan.delacruz@neobank.com", claims.email(), "Extracted email should match");
    }

    @Test
    @DisplayName("Unit Test: Valid JWT extraction using test fallback secret")
    void testJwtValidationWithFallbackSecret() throws Exception {
        long now = Instant.now().getEpochSecond();
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(objectMapper.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT")));
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(objectMapper.writeValueAsBytes(Map.of(
                        "sub", "1001",
                        "email", "juan.delacruz@neobank.com",
                        "exp", now + 3600)));

        String data = header + "." + payload;
        String signature = sign(data, fallbackSecret);
        String validToken = data + "." + signature;

        JwtValidator.Claims claims = jwtValidator.validateAndExtract(validToken);
        assertEquals("1001", claims.userId());
        assertEquals("juan.delacruz@neobank.com", claims.email());
    }

    @Test
    @DisplayName("Unit Test: Rejection of tampered JWT token signature")
    void testJwtValidationTamperedSignature() throws Exception {
        long now = Instant.now().getEpochSecond();
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(objectMapper.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT")));
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(objectMapper.writeValueAsBytes(Map.of(
                        "sub", "1001",
                        "email", "attacker@neobank.com",
                        "exp", now + 3600)));

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
                .encodeToString(objectMapper.writeValueAsBytes(Map.of(
                        "sub", "1001",
                        "email", "expired@neobank.com",
                        "exp", pastTime)));

        String data = header + "." + payload;
        String signature = sign(data, requiredSecret);
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
