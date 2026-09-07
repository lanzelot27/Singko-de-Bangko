package com.singkodebangko.auth_service.controller;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.singkodebangko.auth_service.dto.ErrorResponse;
import com.singkodebangko.auth_service.dto.LoginRequest;
import com.singkodebangko.auth_service.dto.LoginResponse;
import com.singkodebangko.auth_service.model.User;
import com.singkodebangko.auth_service.security.JwtService;
import com.singkodebangko.auth_service.service.UserService;

import jakarta.validation.Valid;

@RestController
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    public AuthController(
            UserService userService,
            JwtService jwtService
    ) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request
    ) {

        User user = userService.findByEmail(request.getEmail());

        if (user == null ||
                !userService.checkPassword(
                        request.getPassword(),
                        user.getPassword()
                )) {

            ErrorResponse errorResponse = new ErrorResponse(
                    Instant.now().toString(),
                    401,
                    "Unauthorized",
                    "Invalid email or password",
                    "/login"
            );

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(errorResponse);
        }

        String token = jwtService.generateToken(
                user.getUserId(),
                user.getEmail()
        );

        LoginResponse response = new LoginResponse(
                token,
                "Bearer",
                86400,
                user.getUserId(),
                user.getEmail()
        );

        return ResponseEntity.ok(response);
    }
}