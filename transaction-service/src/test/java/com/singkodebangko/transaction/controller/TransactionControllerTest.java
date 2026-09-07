package com.singkodebangko.transaction.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.singkodebangko.transaction.client.AccountClient;
import com.singkodebangko.transaction.dto.TransactionHistoryResponse;
import com.singkodebangko.transaction.dto.TransferRequest;
import com.singkodebangko.transaction.dto.TransferResponse;
import com.singkodebangko.transaction.exception.AccountNotFoundException;
import com.singkodebangko.transaction.exception.InsufficientBalanceException;
import com.singkodebangko.transaction.security.JwtTokenProvider;
import com.singkodebangko.transaction.service.TransactionService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private AccountClient accountClient;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String authHeader;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        authHeader = "Bearer " + jwtTokenProvider.generateToken(1001L, "juan.delacruz@neobank.com", 3600);
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when Authorization header is missing")
    void missingAuthorizationHeader_Returns401() throws Exception {
        TransferRequest request = new TransferRequest(
                2001L, 2002L, new BigDecimal("100.00"), "PHP", "Transfer"
        );

        mockMvc.perform(post("/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Should return 201 Created on successful transfer")
    void transfer_Success() throws Exception {
        TransferRequest request = new TransferRequest(
                2001L, 2002L, new BigDecimal("500.00"), "PHP", "Payment for lunch"
        );

        TransferResponse response = new TransferResponse(
                "TXN-20260907-8891", 2001L, 2002L,
                new BigDecimal("500.00"), "PHP", "COMPLETED", "Payment for lunch", Instant.now()
        );

        when(transactionService.executeTransfer(any(TransferRequest.class))).thenReturn(response);

        mockMvc.perform(post("/transactions/transfer")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value("TXN-20260907-8891"))
                .andExpect(jsonPath("$.sourceAccountId").value(2001))
                .andExpect(jsonPath("$.destinationAccountId").value(2002))
                .andExpect(jsonPath("$.amount").value(500.00))
                .andExpect(jsonPath("$.currency").value("PHP"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when debit fails due to insufficient balance")
    void transfer_InsufficientBalance_Returns400() throws Exception {
        TransferRequest request = new TransferRequest(
                2001L, 2002L, new BigDecimal("999999.00"), "PHP", "Payment"
        );

        when(transactionService.executeTransfer(any(TransferRequest.class)))
                .thenThrow(new InsufficientBalanceException("Insufficient funds in account 2001 to execute debit"));

        mockMvc.perform(post("/transactions/transfer")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Insufficient funds in account 2001 to execute debit"));
    }

    @Test
    @DisplayName("Should return 404 Not Found when account does not exist")
    void transfer_AccountNotFound_Returns404() throws Exception {
        TransferRequest request = new TransferRequest(
                9999L, 2002L, new BigDecimal("100.00"), "PHP", "Payment"
        );

        when(transactionService.executeTransfer(any(TransferRequest.class)))
                .thenThrow(new AccountNotFoundException("Source account ID 9999 not found"));

        mockMvc.perform(post("/transactions/transfer")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Source account ID 9999 not found"));
    }

    @Test
    @DisplayName("Should return 200 OK and list of transaction history")
    void getHistory_Success() throws Exception {
        List<TransactionHistoryResponse> history = List.of(
                new TransactionHistoryResponse(
                        "TXN-20260907-8891", 2001L, 2002L,
                        new BigDecimal("500.00"), "PHP", "COMPLETED", Instant.now()
                )
        );

        when(transactionService.getTransactionHistory(2001L)).thenReturn(history);

        mockMvc.perform(get("/transactions/account/2001")
                        .header(HttpHeaders.AUTHORIZATION, authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].transactionId").value("TXN-20260907-8891"))
                .andExpect(jsonPath("$[0].sourceAccountId").value(2001))
                .andExpect(jsonPath("$[0].destinationAccountId").value(2002))
                .andExpect(jsonPath("$[0].amount").value(500.00))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }
}
