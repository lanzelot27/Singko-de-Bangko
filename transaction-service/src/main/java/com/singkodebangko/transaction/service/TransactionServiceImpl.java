package com.singkodebangko.transaction.service;

import com.singkodebangko.transaction.client.AccountClient;
import com.singkodebangko.transaction.dto.AdjustBalanceRequest;
import com.singkodebangko.transaction.dto.TransactionHistoryResponse;
import com.singkodebangko.transaction.dto.TransferRequest;
import com.singkodebangko.transaction.dto.TransferResponse;
import com.singkodebangko.transaction.exception.AccountNotFoundException;
import com.singkodebangko.transaction.exception.InsufficientBalanceException;
import com.singkodebangko.transaction.exception.InvalidTransactionException;
import com.singkodebangko.transaction.exception.TransferProcessingException;
import com.singkodebangko.transaction.model.TransactionRecord;
import com.singkodebangko.transaction.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class TransactionServiceImpl implements TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionServiceImpl.class);

    private final AccountClient accountClient;
    private final TransactionRepository transactionRepository;

    public TransactionServiceImpl(AccountClient accountClient, TransactionRepository transactionRepository) {
        this.accountClient = accountClient;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public TransferResponse executeTransfer(TransferRequest request) {
        // 1. Domain Validations
        if (request.sourceAccountId().equals(request.destinationAccountId())) {
            throw new InvalidTransactionException("Source and destination accounts must not be identical");
        }
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Transfer amount must be greater than zero");
        }

        // 2. Validate existence of both accounts via OpenFeign with clear distinction
        log.info("Verifying source account {}", request.sourceAccountId());
        try {
            accountClient.getProfile(request.sourceAccountId());
        } catch (AccountNotFoundException e) {
            throw new AccountNotFoundException("Source account ID " + request.sourceAccountId() + " not found. Please verify the source account number.");
        }

        log.info("Verifying destination target account {}", request.destinationAccountId());
        try {
            accountClient.getProfile(request.destinationAccountId());
        } catch (AccountNotFoundException e) {
            throw new AccountNotFoundException("Destination account ID " + request.destinationAccountId() + " not found. Transfer cannot be completed because destination account does not exist.");
        }

        // Format aligned with contract: TXN-yyyyMMdd-XXXX
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int randomPart = ThreadLocalRandom.current().nextInt(1000, 9999);
        String transactionId = "TXN-" + datePart + "-" + randomPart;

        // 3. Step 1: Debit source account (negative amount indicates deduction)
        log.info("Debiting {} from source account {}", request.amount(), request.sourceAccountId());
        try {
            accountClient.adjustBalance(
                    request.sourceAccountId(),
                    new AdjustBalanceRequest(transactionId, request.amount().negate(), "DEBIT")
            );
        } catch (InsufficientBalanceException e) {
            throw new InsufficientBalanceException("Insufficient funds in account " + request.sourceAccountId() + " to execute debit");
        }

        // 4. Step 2: Credit destination account with compensation rollback on failure
        try {
            log.info("Crediting {} to destination account {}", request.amount(), request.destinationAccountId());
            accountClient.adjustBalance(
                    request.destinationAccountId(),
                    new AdjustBalanceRequest(transactionId, request.amount(), "CREDIT")
            );
        } catch (AccountNotFoundException creditNotFound) {
            log.error("Destination account {} not found during credit. Rolling back source account {}",
                    request.destinationAccountId(), request.sourceAccountId());
            rollbackSourceDebit(request, transactionId);
            throw new AccountNotFoundException("Destination account ID " + request.destinationAccountId() + " not found. Transfer cannot be completed because destination account does not exist.");
        } catch (Exception creditException) {
            log.error("Failed to credit destination account {}. Initiating rollback compensation for source account {}",
                    request.destinationAccountId(), request.sourceAccountId(), creditException);
            rollbackSourceDebit(request, transactionId);

            // Persist failed transaction record for audit trail
            TransactionRecord failedRecord = new TransactionRecord(
                    transactionId,
                    request.sourceAccountId(),
                    request.destinationAccountId(),
                    request.amount(),
                    request.currency(),
                    "FAILED",
                    "Compensated: " + creditException.getMessage(),
                    Instant.now()
            );
            transactionRepository.save(failedRecord);

            throw new TransferProcessingException(
                    "Transfer failed during destination credit; source debit has been rolled back: " + creditException.getMessage(),
                    creditException
            );
        }

        // 5. Successful transfer: persist and return 201 Created
        TransactionRecord record = new TransactionRecord(
                transactionId,
                request.sourceAccountId(),
                request.destinationAccountId(),
                request.amount(),
                request.currency(),
                "COMPLETED",
                request.description() != null ? request.description() : "Transfer",
                Instant.now()
        );
        transactionRepository.save(record);

        log.info("Transfer {} completed successfully from {} to {} for amount {} {}",
                transactionId, request.sourceAccountId(), request.destinationAccountId(), request.amount(), request.currency());

        return new TransferResponse(
                record.getTransactionId(),
                record.getSourceAccountId(),
                record.getDestinationAccountId(),
                record.getAmount(),
                record.getCurrency(),
                record.getStatus(),
                record.getDescription(),
                record.getTimestamp()
        );
    }

    @Override
    public List<TransactionHistoryResponse> getTransactionHistory(Long accountId) {
        // Validate account existence
        accountClient.getProfile(accountId);

        return transactionRepository.findByAccountId(accountId).stream()
                .map(t -> new TransactionHistoryResponse(
                        t.getTransactionId(),
                        t.getSourceAccountId(),
                        t.getDestinationAccountId(),
                        t.getAmount(),
                        t.getCurrency(),
                        t.getStatus(),
                        t.getTimestamp()
                ))
                .collect(Collectors.toList());
    }

    private void rollbackSourceDebit(TransferRequest request, String transactionId) {
        try {
            accountClient.adjustBalance(
                    request.sourceAccountId(),
                    new AdjustBalanceRequest(transactionId + "-ROLLBACK", request.amount(), "CREDIT")
            );
            log.info("Rollback compensation completed successfully for source account {}", request.sourceAccountId());
        } catch (Exception rollbackException) {
            log.error("CRITICAL: Rollback compensation failed for account {}: {}",
                    request.sourceAccountId(), rollbackException.getMessage(), rollbackException);
        }
    }
}
