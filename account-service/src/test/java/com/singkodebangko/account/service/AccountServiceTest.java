package com.singkodebangko.account.service;

import com.singkodebangko.account.dto.AccountBalanceResponse;
import com.singkodebangko.account.dto.AccountProfileResponse;
import com.singkodebangko.account.dto.AdjustBalanceRequest;
import com.singkodebangko.account.dto.AdjustBalanceResponse;
import com.singkodebangko.account.exception.AccountNotFoundException;
import com.singkodebangko.account.exception.InsufficientBalanceException;
import com.singkodebangko.account.model.Account;
import com.singkodebangko.account.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountServiceImpl accountService;

    private Account sampleAccount;

    @BeforeEach
    void setUp() {
        sampleAccount = new Account(
                2001L,
                "ACC-987654321",
                1001L,
                "SAVINGS",
                "Juan Dela Cruz",
                "juan.delacruz@neobank.com",
                new BigDecimal("15000.75"),
                "PHP",
                "ACTIVE",
                Instant.now()
        );
    }

    @Test
    @DisplayName("Should return balance when account exists")
    void getBalance_Success() {
        when(accountRepository.findById(2001L)).thenReturn(Optional.of(sampleAccount));

        AccountBalanceResponse response = accountService.getBalance(2001L);

        assertNotNull(response);
        assertEquals(2001L, response.accountId());
        assertEquals("ACC-987654321", response.accountNumber());
        assertEquals(new BigDecimal("15000.75"), response.balance());
        assertEquals("PHP", response.currency());
    }

    @Test
    @DisplayName("Should throw AccountNotFoundException when balance requested for missing account")
    void getBalance_NotFound() {
        when(accountRepository.findById(9999L)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> accountService.getBalance(9999L));
    }

    @Test
    @DisplayName("Should return profile when account exists")
    void getProfile_Success() {
        when(accountRepository.findById(2001L)).thenReturn(Optional.of(sampleAccount));

        AccountProfileResponse response = accountService.getProfile(2001L);

        assertNotNull(response);
        assertEquals(2001L, response.accountId());
        assertEquals("Juan Dela Cruz", response.ownerName());
        assertEquals("juan.delacruz@neobank.com", response.email());
        assertEquals("ACTIVE", response.status());
    }

    @Test
    @DisplayName("Should successfully adjust balance for DEBIT")
    void adjustBalance_Debit_Success() {
        when(accountRepository.findById(2001L)).thenReturn(Optional.of(sampleAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdjustBalanceRequest request = new AdjustBalanceRequest("TXN-123", new BigDecimal("-500.00"), "DEBIT");
        AdjustBalanceResponse response = accountService.adjustBalance(2001L, request);

        assertNotNull(response);
        assertEquals(2001L, response.accountId());
        assertEquals(new BigDecimal("14500.75"), response.newBalance());
    }

    @Test
    @DisplayName("Should successfully adjust balance for CREDIT")
    void adjustBalance_Credit_Success() {
        when(accountRepository.findById(2001L)).thenReturn(Optional.of(sampleAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdjustBalanceRequest request = new AdjustBalanceRequest("TXN-123", new BigDecimal("500.00"), "CREDIT");
        AdjustBalanceResponse response = accountService.adjustBalance(2001L, request);

        assertNotNull(response);
        assertEquals(2001L, response.accountId());
        assertEquals(new BigDecimal("15500.75"), response.newBalance());
    }

    @Test
    @DisplayName("Should throw InsufficientBalanceException when debit exceeds balance")
    void adjustBalance_InsufficientFunds() {
        when(accountRepository.findById(2001L)).thenReturn(Optional.of(sampleAccount));

        AdjustBalanceRequest request = new AdjustBalanceRequest("TXN-123", new BigDecimal("-999999.00"), "DEBIT");

        assertThrows(InsufficientBalanceException.class, () -> accountService.adjustBalance(2001L, request));
    }
}
