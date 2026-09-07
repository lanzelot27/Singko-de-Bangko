package com.singkodebangko.transaction.repository;

import com.singkodebangko.transaction.model.TransactionRecord;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryTransactionRepository implements TransactionRepository {

    private final Map<String, TransactionRecord> store = new ConcurrentHashMap<>();

    @Override
    public TransactionRecord save(TransactionRecord transaction) {
        if (transaction == null || transaction.getTransactionId() == null) {
            throw new IllegalArgumentException("Transaction and its ID cannot be null");
        }
        store.put(transaction.getTransactionId(), transaction);
        return transaction;
    }

    @Override
    public Optional<TransactionRecord> findById(String transactionId) {
        if (transactionId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(store.get(transactionId));
    }

    @Override
    public List<TransactionRecord> findByAccountId(Long accountId) {
        if (accountId == null) {
            return List.of();
        }
        return store.values().stream()
                .filter(t -> accountId.equals(t.getSourceAccountId()) || accountId.equals(t.getDestinationAccountId()))
                .sorted(Comparator.comparing(TransactionRecord::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<TransactionRecord> findAll() {
        return new ArrayList<>(store.values());
    }
}
