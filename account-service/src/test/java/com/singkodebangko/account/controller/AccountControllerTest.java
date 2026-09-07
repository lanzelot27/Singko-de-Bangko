package com.singkodebangko.account.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.singkodebangko.account.dto.AccountBalanceResponse;
import com.singkodebangko.account.dto.AccountProfileResponse;
import com.singkodebangko.account.dto.AdjustBalanceRequest;
import com.singkodebangko.account.dto.AdjustBalanceResponse;
import com.singkodebangko.account.exception.AccountNotFoundException;
import com.singkodebangko.account.exception.InsufficientBalanceException;
import com.singkodebangko.account.service.AccountService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

    private static final String AUTH_HEADER = "Bearer test-jwt-token";

    @Test
    @DisplayName("Should return 401 Unauthorized when Authorization header is missing")
    void missingAuthorizationHeader_Returns401() throws Exception {
        mockMvc.perform(get("/accounts/2001/balance"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Should return 200 OK and balance with valid Authorization header")
    void getBalance_Success() throws Exception {
        when(accountService.getBalance(2001L)).thenReturn(
                new AccountBalanceResponse(2001L, "ACC-987654321", 1001L, "PHP", new BigDecimal("15000.75"), "ACTIVE", Instant.now())
        );

        mockMvc.perform(get("/accounts/2001/balance")
                        .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(2001))
                .andExpect(jsonPath("$.accountNumber").value("ACC-987654321"))
                .andExpect(jsonPath("$.userId").value(1001))
                .andExpect(jsonPath("$.balance").value(15000.75))
                .andExpect(jsonPath("$.currency").value("PHP"));
    }

    @Test
    @DisplayName("Should return 404 Not Found when account does not exist")
    void getBalance_NotFound() throws Exception {
        when(accountService.getBalance(9999L))
                .thenThrow(new AccountNotFoundException("Account ID 9999 not found"));

        mockMvc.perform(get("/accounts/9999/balance")
                        .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Account ID 9999 not found"));
    }

    @Test
    @DisplayName("Should return 200 OK and profile details")
    void getProfile_Success() throws Exception {
        when(accountService.getProfile(2001L)).thenReturn(
                new AccountProfileResponse(2001L, "ACC-987654321", "SAVINGS", "Juan Dela Cruz", "juan.delacruz@neobank.com", "ACTIVE")
        );

        mockMvc.perform(get("/accounts/2001/profile")
                        .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(2001))
                .andExpect(jsonPath("$.ownerName").value("Juan Dela Cruz"))
                .andExpect(jsonPath("$.email").value("juan.delacruz@neobank.com"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Should return 200 OK on successful balance adjustment")
    void adjustBalance_Success() throws Exception {
        AdjustBalanceRequest request = new AdjustBalanceRequest("TXN-101", new BigDecimal("-500.00"), "DEBIT");
        when(accountService.adjustBalance(eq(2001L), any(AdjustBalanceRequest.class))).thenReturn(
                new AdjustBalanceResponse(2001L, new BigDecimal("14500.75"), Instant.now())
        );

        mockMvc.perform(put("/accounts/2001/adjust-balance")
                        .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(2001))
                .andExpect(jsonPath("$.newBalance").value(14500.75));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when adjustBalance has insufficient funds")
    void adjustBalance_InsufficientFunds() throws Exception {
        AdjustBalanceRequest request = new AdjustBalanceRequest("TXN-101", new BigDecimal("-999999.00"), "DEBIT");
        when(accountService.adjustBalance(eq(2001L), any(AdjustBalanceRequest.class)))
                .thenThrow(new InsufficientBalanceException("Insufficient funds to execute debit"));

        mockMvc.perform(put("/accounts/2001/adjust-balance")
                        .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Insufficient funds to execute debit"));
    }
}
