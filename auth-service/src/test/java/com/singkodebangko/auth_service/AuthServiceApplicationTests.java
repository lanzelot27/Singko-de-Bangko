package com.singkodebangko.auth_service;

import com.singkodebangko.auth_service.dto.LoginRequest;
import com.singkodebangko.auth_service.dto.LoginResponse;
import com.singkodebangko.auth_service.dto.ErrorResponse;
import com.singkodebangko.auth_service.controller.AuthController;
import com.singkodebangko.auth_service.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AuthServiceApplicationTests {

    @Autowired
    private AuthController authController;

    @Autowired
    private JwtService jwtService;

    @Test
    void contextLoads() {
        assertNotNull(authController);
        assertNotNull(jwtService);
    }

    @Test
    @DisplayName("TC-AUTH-01: Valid login returns 200 OK with JWT token and contract claims")
    void testLoginSuccess() {
        LoginRequest request = new LoginRequest();
        request.setEmail("juan.delacruz@neobank.com");
        request.setPassword("Password123!");

        ResponseEntity<?> response = authController.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof LoginResponse);

        LoginResponse body = (LoginResponse) response.getBody();
        assertNotNull(body.getAccessToken());
        assertFalse(body.getAccessToken().isBlank());
        assertEquals("Bearer", body.getTokenType());
        assertEquals(86400, body.getExpiresIn());
        assertEquals(1001L, body.getUserId());
        assertEquals("juan.delacruz@neobank.com", body.getEmail());
    }

    @Test
    @DisplayName("TC-AUTH-02: Invalid credentials return 401 Unauthorized with standard error body")
    void testLoginInvalidCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("juan.delacruz@neobank.com");
        request.setPassword("WrongPassword!");

        ResponseEntity<?> response = authController.login(request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody() instanceof ErrorResponse);

        ErrorResponse error = (ErrorResponse) response.getBody();
        assertEquals(401, error.getStatus());
        assertEquals("Unauthorized", error.getError());
        assertEquals("Invalid email or password", error.getMessage());
        assertEquals("/login", error.getPath());
        assertNotNull(error.getTimestamp());
    }

    @Test
    @DisplayName("TC-AUTH-03: Unknown user returns 401 Unauthorized")
    void testLoginUnknownUser() {
        LoginRequest request = new LoginRequest();
        request.setEmail("nonexistent@neobank.com");
        request.setPassword("Password123!");

        ResponseEntity<?> response = authController.login(request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody() instanceof ErrorResponse);
    }

    @Test
    @DisplayName("TC-AUTH-04: JWT generation generates non-empty compact token")
    void testJwtGeneration() {
        String token = jwtService.generateToken(1001L, "juan.delacruz@neobank.com");
        assertNotNull(token);
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT must consist of header, payload, and signature");
    }
}
