package com.singkodebangko.transaction.repository;

import com.singkodebangko.transaction.model.TransactionRecord;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository {
    TransactionRecord save(TransactionRecord transaction);
    Optional<TransactionRecord> findById(String transactionId);
    List<TransactionRecord> findByAccountId(Long accountId);
    List<TransactionRecord> findAll();
}
