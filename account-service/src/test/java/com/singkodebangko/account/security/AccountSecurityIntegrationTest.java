package com.singkodebangko.account.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("TC-ACC-01: Authenticated Balance Inquiry (Technical)")
    void tcAcc01_AuthenticatedBalanceInquiry() throws Exception {
        // Valid token for Juan Dela Cruz (User ID: 1001)
        String validToken = "Bearer " + jwtTokenProvider.generateToken(1001L, "juan.delacruz@neobank.com", 3600);

        mockMvc.perform(get("/accounts/2001/balance")
                        .header(HttpHeaders.AUTHORIZATION, validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(2001))
                .andExpect(jsonPath("$.accountNumber").value("ACC-987654321"))
                .andExpect(jsonPath("$.userId").value(1001))
                .andExpect(jsonPath("$.currency").value("PHP"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("TC-ACC-02: Identity Context from JWT Claims (Code Quality)")
    void tcAcc02_IdentityContextFromJwtClaims() throws Exception {
        // 1. Matching Identity Context: User 1001 accessing own account 2001 -> 200 OK
        String juanToken = "Bearer " + jwtTokenProvider.generateToken(1001L, "juan.delacruz@neobank.com", 3600);
        mockMvc.perform(get("/accounts/2001/balance")
                        .header(HttpHeaders.AUTHORIZATION, juanToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1001));

        // 2. Mismatched Identity Context: User 1002 (Maria Santos) attempting to query Account 2001 (owned by 1001) -> 403 Forbidden
        String mariaToken = "Bearer " + jwtTokenProvider.generateToken(1002L, "maria.santos@neobank.com", 3600);
        mockMvc.perform(get("/accounts/2001/balance")
                        .header(HttpHeaders.AUTHORIZATION, mariaToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Access denied: Authenticated user 1002 is not authorized to access account 2001"));
    }

    @Test
    @DisplayName("TC-ACC-03: Tampered Signature Rejected (Security)")
    void tcAcc03_TamperedSignatureRejected() throws Exception {
        // Generate valid token
        String validRawToken = jwtTokenProvider.generateToken(1001L, "juan.delacruz@neobank.com", 3600);

        // Tamper with signature by altering the last characters
        String tamperedToken = "Bearer " + validRawToken.substring(0, validRawToken.length() - 4) + "X9Z1";

        mockMvc.perform(get("/accounts/2001/balance")
                        .header(HttpHeaders.AUTHORIZATION, tamperedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid or tampered JWT signature"));
    }
}
