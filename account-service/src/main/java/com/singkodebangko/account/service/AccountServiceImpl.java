package com.singkodebangko.account.service;

import com.singkodebangko.account.dto.AccountBalanceResponse;
import com.singkodebangko.account.dto.AccountProfileResponse;
import com.singkodebangko.account.dto.AdjustBalanceRequest;
import com.singkodebangko.account.dto.AdjustBalanceResponse;
import com.singkodebangko.account.exception.AccountNotFoundException;
import com.singkodebangko.account.exception.InsufficientBalanceException;
import com.singkodebangko.account.model.Account;
import com.singkodebangko.account.repository.AccountRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    public AccountServiceImpl(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public AccountBalanceResponse getBalance(Long accountId) {
        Account account = findAccountOrThrow(accountId);
        validateAccountOwnership(account);
        return new AccountBalanceResponse(
                account.getAccountId(),
                account.getAccountNumber(),
                account.getUserId(),
                account.getCurrency(),
                account.getBalance(),
                account.getStatus(),
                account.getUpdatedAt()
        );
    }

    @Override
    public AccountProfileResponse getProfile(Long accountId) {
        Account account = findAccountOrThrow(accountId);
        return new AccountProfileResponse(
                account.getAccountId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getOwnerName(),
                account.getEmail(),
                account.getStatus()
        );
    }

    private void validateAccountOwnership(Account account) {
        Long currentUserId = com.singkodebangko.account.security.UserContextHolder.getCurrentUserId();
        if (currentUserId != null && !currentUserId.equals(account.getUserId())) {
            throw new com.singkodebangko.account.exception.AccessDeniedException(
                    "Access denied: Authenticated user " + currentUserId + " is not authorized to access account " + account.getAccountId()
            );
        }
    }

    @Override
    public AdjustBalanceResponse adjustBalance(Long accountId, AdjustBalanceRequest request) {
        Account account = findAccountOrThrow(accountId);

        synchronized (account) {
            BigDecimal currentBalance = account.getBalance();
            BigDecimal adjustment = request.amount();
            BigDecimal newBalance = currentBalance.add(adjustment);

            // Negative new balance signifies insufficient funds for debit
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new InsufficientBalanceException("Insufficient funds to execute debit");
            }

            account.setBalance(newBalance);
            account.setUpdatedAt(Instant.now());
            accountRepository.save(account);

            return new AdjustBalanceResponse(
                    account.getAccountId(),
                    newBalance,
                    account.getUpdatedAt()
            );
        }
    }

    private Account findAccountOrThrow(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account ID " + accountId + " not found"));
    }
}
