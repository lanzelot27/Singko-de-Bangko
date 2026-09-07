package com.singkodebangko.transaction.service;

import com.singkodebangko.transaction.client.AccountClient;
import com.singkodebangko.transaction.dto.AccountProfileResponse;
import com.singkodebangko.transaction.dto.AdjustBalanceRequest;
import com.singkodebangko.transaction.dto.AdjustBalanceResponse;
import com.singkodebangko.transaction.dto.TransactionHistoryResponse;
import com.singkodebangko.transaction.dto.TransferRequest;
import com.singkodebangko.transaction.dto.TransferResponse;
import com.singkodebangko.transaction.exception.AccountNotFoundException;
import com.singkodebangko.transaction.exception.InsufficientBalanceException;
import com.singkodebangko.transaction.exception.InvalidTransactionException;
import com.singkodebangko.transaction.exception.TransferProcessingException;
import com.singkodebangko.transaction.model.TransactionRecord;
import com.singkodebangko.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private AccountClient accountClient;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private TransferRequest validTransferRequest;

    @BeforeEach
    void setUp() {
        validTransferRequest = new TransferRequest(
                2001L,
                2002L,
                new BigDecimal("500.00"),
                "PHP",
                "Payment for lunch"
        );
    }

    @Test
    @DisplayName("Should successfully transfer funds between accounts")
    void executeTransfer_Success() {
        when(accountClient.getProfile(2001L))
                .thenReturn(new AccountProfileResponse(2001L, "ACC-987654321", "SAVINGS", "Juan Dela Cruz", "juan@example.com", "ACTIVE"));
        when(accountClient.getProfile(2002L))
                .thenReturn(new AccountProfileResponse(2002L, "ACC-123456789", "SAVINGS", "Maria Santos", "maria@example.com", "ACTIVE"));

        when(accountClient.adjustBalance(eq(2001L), any(AdjustBalanceRequest.class)))
                .thenReturn(new AdjustBalanceResponse(2001L, new BigDecimal("14500.75"), Instant.now()));
        when(accountClient.adjustBalance(eq(2002L), any(AdjustBalanceRequest.class)))
                .thenReturn(new AdjustBalanceResponse(2002L, new BigDecimal("25500.00"), Instant.now()));

        when(transactionRepository.save(any(TransactionRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        TransferResponse response = transactionService.executeTransfer(validTransferRequest);

        assertNotNull(response);
        assertNotNull(response.transactionId());
        assertTrue(response.transactionId().startsWith("TXN-"));
        assertEquals(2001L, response.sourceAccountId());
        assertEquals(2002L, response.destinationAccountId());
        assertEquals(new BigDecimal("500.00"), response.amount());
        assertEquals("PHP", response.currency());
        assertEquals("COMPLETED", response.status());

        verify(accountClient).adjustBalance(eq(2001L), argThat(r -> r.amount().compareTo(new BigDecimal("-500.00")) == 0));
        verify(accountClient).adjustBalance(eq(2002L), argThat(r -> r.amount().compareTo(new BigDecimal("500.00")) == 0));
        verify(transactionRepository).save(any(TransactionRecord.class));
    }

    @Test
    @DisplayName("Should reject transfer if source and destination accounts are identical")
    void executeTransfer_SameAccount() {
        TransferRequest invalidRequest = new TransferRequest(
                2001L,
                2001L,
                new BigDecimal("100.00"),
                "PHP",
                "Invalid self transfer"
        );

        assertThrows(InvalidTransactionException.class, () -> transactionService.executeTransfer(invalidRequest));
        verifyNoInteractions(accountClient);
    }

    @Test
    @DisplayName("Should throw AccountNotFoundException when source account does not exist")
    void executeTransfer_SourceAccountNotFound() {
        when(accountClient.getProfile(2001L))
                .thenThrow(new AccountNotFoundException("Account ID 2001 not found"));

        assertThrows(AccountNotFoundException.class, () -> transactionService.executeTransfer(validTransferRequest));
        verify(accountClient, never()).adjustBalance(any(), any());
    }

    @Test
    @DisplayName("Should throw AccountNotFoundException when destination account does not exist")
    void executeTransfer_DestinationAccountNotFound() {
        when(accountClient.getProfile(2001L))
                .thenReturn(new AccountProfileResponse(2001L, "ACC-987654321", "SAVINGS", "Juan Dela Cruz", "juan@example.com", "ACTIVE"));
        when(accountClient.getProfile(2002L))
                .thenThrow(new AccountNotFoundException("Account ID 2002 not found"));

        AccountNotFoundException ex = assertThrows(AccountNotFoundException.class,
                () -> transactionService.executeTransfer(validTransferRequest));
        assertTrue(ex.getMessage().contains("Destination account ID"));
        verify(accountClient, never()).adjustBalance(any(), any());
    }

    @Test
    @DisplayName("Should propagate InsufficientBalanceException when debit fails")
    void executeTransfer_InsufficientBalance() {
        when(accountClient.getProfile(2001L))
                .thenReturn(new AccountProfileResponse(2001L, "ACC-987654321", "SAVINGS", "Juan Dela Cruz", "juan@example.com", "ACTIVE"));
        when(accountClient.getProfile(2002L))
                .thenReturn(new AccountProfileResponse(2002L, "ACC-123456789", "SAVINGS", "Maria Santos", "maria@example.com", "ACTIVE"));

        when(accountClient.adjustBalance(eq(2001L), any(AdjustBalanceRequest.class)))
                .thenThrow(new InsufficientBalanceException("Insufficient funds to execute debit"));

        assertThrows(InsufficientBalanceException.class, () -> transactionService.executeTransfer(validTransferRequest));
        verify(accountClient, never()).adjustBalance(eq(2002L), any());
    }

    @Test
    @DisplayName("Should execute rollback compensation when credit to destination fails")
    void executeTransfer_RollbackCompensation() {
        when(accountClient.getProfile(2001L))
                .thenReturn(new AccountProfileResponse(2001L, "ACC-987654321", "SAVINGS", "Juan Dela Cruz", "juan@example.com", "ACTIVE"));
        when(accountClient.getProfile(2002L))
                .thenReturn(new AccountProfileResponse(2002L, "ACC-123456789", "SAVINGS", "Maria Santos", "maria@example.com", "ACTIVE"));

        when(accountClient.adjustBalance(eq(2001L), any(AdjustBalanceRequest.class)))
                .thenReturn(new AdjustBalanceResponse(2001L, new BigDecimal("14500.75"), Instant.now()));

        // Destination credit fails
        when(accountClient.adjustBalance(eq(2002L), any(AdjustBalanceRequest.class)))
                .thenThrow(new RuntimeException("Downstream network timeout"));

        assertThrows(TransferProcessingException.class, () -> transactionService.executeTransfer(validTransferRequest));

        // Verify compensating credit was issued back to source account
        verify(accountClient, times(2)).adjustBalance(eq(2001L), any(AdjustBalanceRequest.class));
        verify(transactionRepository).save(argThat(record -> "FAILED".equals(record.getStatus())));
    }

    @Test
    @DisplayName("Should return transaction history for account")
    void getTransactionHistory_Success() {
        when(accountClient.getProfile(2001L))
                .thenReturn(new AccountProfileResponse(2001L, "ACC-987654321", "SAVINGS", "Juan Dela Cruz", "juan@example.com", "ACTIVE"));

        TransactionRecord record1 = new TransactionRecord(
                "TXN-20260907-8891", 2001L, 2002L, new BigDecimal("500.00"),
                "PHP", "COMPLETED", "Payment for lunch", Instant.now()
        );

        when(transactionRepository.findByAccountId(2001L)).thenReturn(List.of(record1));

        List<TransactionHistoryResponse> history = transactionService.getTransactionHistory(2001L);

        assertNotNull(history);
        assertEquals(1, history.size());
        assertEquals("TXN-20260907-8891", history.get(0).transactionId());
        assertEquals(2001L, history.get(0).sourceAccountId());
        assertEquals(2002L, history.get(0).destinationAccountId());
    }
}
