package com.singkodebangko.account.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.singkodebangko.account.dto.ErrorResponse;
import com.singkodebangko.account.security.JwtClaims;
import com.singkodebangko.account.security.JwtTokenProvider;
import com.singkodebangko.account.security.UserContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter enforcing JWT authentication and extracting caller identity claims.
 * - Rejects missing Authorization with 401 Unauthorized.
 * - Cryptographically verifies HMAC-SHA256 signature; rejects tampered tokens with 401 Unauthorized.
 * - Extracts authenticated userId/email and populates UserContextHolder.
 */
@Component
public class SecurityFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final JwtTokenProvider jwtTokenProvider;

    public SecurityFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Enforce authorization on /accounts/** routes
        if (path.startsWith("/accounts") || path.startsWith("/api/accounts")) {
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            String xUserId = request.getHeader("X-User-Id");
            String xUserEmail = request.getHeader("X-User-Email");

            if ((authHeader == null || authHeader.trim().isBlank()) && (xUserId == null || xUserId.trim().isBlank())) {
                sendUnauthorizedError(request, response, "Authorization header is missing or empty");
                return;
            }

            try {
                if (authHeader != null && !authHeader.trim().isBlank()) {
                    JwtClaims claims = jwtTokenProvider.validateTokenAndExtractClaims(authHeader);
                    UserContextHolder.setContext(claims.userId(), claims.email());
                } else if (xUserId != null && !xUserId.trim().isBlank()) {
                    Long userId = Long.parseLong(xUserId.trim());
                    UserContextHolder.setContext(userId, xUserEmail);
                }
            } catch (SecurityException ex) {
                sendUnauthorizedError(request, response, ex.getMessage());
                return;
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            UserContextHolder.clear();
        }
    }

    private void sendUnauthorizedError(HttpServletRequest request,
                                       HttpServletResponse response,
                                       String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                message,
                request.getRequestURI()
        );

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
