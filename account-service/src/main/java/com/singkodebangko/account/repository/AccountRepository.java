package com.singkodebangko.account.repository;

import com.singkodebangko.account.model.Account;
import java.util.Optional;

public interface AccountRepository {
    Optional<Account> findById(Long accountId);
    Account save(Account account);
    boolean existsById(Long accountId);
}
