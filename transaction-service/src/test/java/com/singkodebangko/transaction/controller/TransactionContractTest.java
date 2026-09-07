package com.singkodebangko.transaction.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.singkodebangko.transaction.client.AccountClient;
import com.singkodebangko.transaction.dto.AdjustBalanceRequest;
import com.singkodebangko.transaction.dto.AdjustBalanceResponse;
import com.singkodebangko.transaction.dto.TransferRequest;
import com.singkodebangko.transaction.exception.AccountNotFoundException;
import com.singkodebangko.transaction.exception.InsufficientBalanceException;
import com.singkodebangko.transaction.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private AccountClient accountClient;

    private String authToken;

    @BeforeEach
    void setUp() {
        authToken = "Bearer " + jwtTokenProvider.generateToken(1001L, "juan.delacruz@neobank.com", 3600);
    }

    @Test
    @DisplayName("TC-TXN-01: Peer-to-Peer Fund Transfer (Technical)")
    void tcTxn01_PeerToPeerFundTransfer() throws Exception {
        TransferRequest request = new TransferRequest(
                2001L,
                2002L,
                new BigDecimal("500.00"),
                "PHP",
                "Sprint Demo Transfer"
        );

        when(accountClient.adjustBalance(eq(2001L), any(AdjustBalanceRequest.class)))
                .thenReturn(new AdjustBalanceResponse(2001L, new BigDecimal("14500.75"), Instant.now()));

        when(accountClient.adjustBalance(eq(2002L), any(AdjustBalanceRequest.class)))
                .thenReturn(new AdjustBalanceResponse(2002L, new BigDecimal("25500.00"), Instant.now()));

        mockMvc.perform(post("/transactions/transfer")
                        .header(HttpHeaders.AUTHORIZATION, authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").exists())
                .andExpect(jsonPath("$.sourceAccountId").value(2001))
                .andExpect(jsonPath("$.destinationAccountId").value(2002))
                .andExpect(jsonPath("$.amount").value(500.00))
                .andExpect(jsonPath("$.currency").value("PHP"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("TC-TXN-02: Insufficient Funds Validation (Problem Solving)")
    void tcTxn02_InsufficientFundsValidation() throws Exception {
        TransferRequest request = new TransferRequest(
                2003L,
                2002L,
                new BigDecimal("999999.00"),
                "PHP",
                "Overdraft attempt"
        );

        when(accountClient.adjustBalance(eq(2003L), any(AdjustBalanceRequest.class)))
                .thenThrow(new InsufficientBalanceException("Insufficient funds in account 2003 to execute debit"));

        mockMvc.perform(post("/transactions/transfer")
                        .header(HttpHeaders.AUTHORIZATION, authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Insufficient funds in account 2003 to execute debit"));
    }

    @Test
    @DisplayName("TC-TXN-03: Invalid Target Account Rejection (Code Quality)")
    void tcTxn03_InvalidTargetAccountRejection() throws Exception {
        TransferRequest request = new TransferRequest(
                2001L,
                2999L,
                new BigDecimal("100.00"),
                "PHP",
                "Transfer to invalid destination"
        );

        when(accountClient.getProfile(eq(2999L)))
                .thenThrow(new AccountNotFoundException("Destination account ID 2999 not found"));

        when(accountClient.adjustBalance(eq(2001L), any(AdjustBalanceRequest.class)))
                .thenReturn(new AdjustBalanceResponse(2001L, new BigDecimal("14900.75"), Instant.now()));

        when(accountClient.adjustBalance(eq(2999L), any(AdjustBalanceRequest.class)))
                .thenThrow(new AccountNotFoundException("Destination account ID 2999 not found"));

        mockMvc.perform(post("/transactions/transfer")
                        .header(HttpHeaders.AUTHORIZATION, authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Destination account ID 2999 not found. Transfer cannot be completed because destination account does not exist."));
    }
}
