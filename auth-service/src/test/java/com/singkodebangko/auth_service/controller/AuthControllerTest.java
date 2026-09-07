package com.singkodebangko.auth_service.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.http.ResponseEntity;

import com.singkodebangko.auth_service.dto.LoginRequest;
import com.singkodebangko.auth_service.model.User;
import com.singkodebangko.auth_service.security.JwtService;
import com.singkodebangko.auth_service.service.UserService;

class AuthControllerTest {

    @Test
    void loginWithValidCredentialsReturnsToken() {

        UserService userService = mock(UserService.class);
        JwtService jwtService = mock(JwtService.class);

        User user = new User(
                1001L,
                "juan.delacruz@neobank.com",
                "encoded-password"
        );

        when(userService.findByEmail("juan.delacruz@neobank.com"))
                .thenReturn(user);

        when(userService.checkPassword("Password123!", "encoded-password"))
                .thenReturn(true);

        when(jwtService.generateToken(
                1001L,
                "juan.delacruz@neobank.com"
        )).thenReturn("test-jwt-token");

        AuthController controller = new AuthController(
                userService,
                jwtService
        );

        LoginRequest request = new LoginRequest();
        request.setEmail("juan.delacruz@neobank.com");
        request.setPassword("Password123!");

        ResponseEntity<?> response = controller.login(request);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());

        verify(userService).findByEmail("juan.delacruz@neobank.com");
        verify(userService).checkPassword(
                "Password123!",
                "encoded-password"
        );

        verify(jwtService).generateToken(
                1001L,
                "juan.delacruz@neobank.com"
        );
    }

    @Test
    void loginWithInvalidCredentialsReturnsUnauthorized() {

        UserService userService = mock(UserService.class);
        JwtService jwtService = mock(JwtService.class);

        User user = new User(
                1001L,
                "juan.delacruz@neobank.com",
                "encoded-password"
        );

        when(userService.findByEmail("juan.delacruz@neobank.com"))
                .thenReturn(user);

        when(userService.checkPassword(
                "WrongPassword123!",
                "encoded-password"
        )).thenReturn(false);

        AuthController controller = new AuthController(
                userService,
                jwtService
        );

        LoginRequest request = new LoginRequest();
        request.setEmail("juan.delacruz@neobank.com");
        request.setPassword("WrongPassword123!");

        ResponseEntity<?> response = controller.login(request);

        assertEquals(401, response.getStatusCode().value());
        assertNotNull(response.getBody());

        verify(userService).findByEmail("juan.delacruz@neobank.com");

        verify(userService).checkPassword(
                "WrongPassword123!",
                "encoded-password"
        );

        verify(jwtService, never()).generateToken(
                anyLong(),
                anyString()
        );
    }
}