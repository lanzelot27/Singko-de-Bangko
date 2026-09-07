package com.singkodebangko.account.repository;

import com.singkodebangko.account.model.Account;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryAccountRepository implements AccountRepository {

    private final Map<Long, Account> accounts = new ConcurrentHashMap<>();

    @PostConstruct
    public void initSeedData() {
        // Seed 2001: Juan Dela Cruz (matches contract documentation)
        accounts.put(2001L, new Account(
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
        ));

        // Seed 2002: Maria Santos
        accounts.put(2002L, new Account(
                2002L,
                "ACC-123456789",
                1002L,
                "SAVINGS",
                "Maria Santos",
                "maria.santos@neobank.com",
                new BigDecimal("25000.00"),
                "PHP",
                "ACTIVE",
                Instant.now()
        ));

        // Seed 2003: Pedro Penduko
        accounts.put(2003L, new Account(
                2003L,
                "ACC-555666777",
                1003L,
                "CHECKING",
                "Pedro Penduko",
                "pedro.penduko@neobank.com",
                new BigDecimal("500.00"),
                "PHP",
                "ACTIVE",
                Instant.now()
        ));
    }

    @Override
    public Optional<Account> findById(Long accountId) {
        if (accountId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(accounts.get(accountId));
    }

    @Override
    public Account save(Account account) {
        if (account == null || account.getAccountId() == null) {
            throw new IllegalArgumentException("Account and its ID must not be null");
        }
        accounts.put(account.getAccountId(), account);
        return account;
    }

    @Override
    public boolean existsById(Long accountId) {
        return accountId != null && accounts.containsKey(accountId);
    }
}
