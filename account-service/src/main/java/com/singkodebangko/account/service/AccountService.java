package com.singkodebangko.account.service;

import com.singkodebangko.account.dto.AccountBalanceResponse;
import com.singkodebangko.account.dto.AccountProfileResponse;
import com.singkodebangko.account.dto.AdjustBalanceRequest;
import com.singkodebangko.account.dto.AdjustBalanceResponse;

public interface AccountService {
    AccountBalanceResponse getBalance(Long accountId);
    AccountProfileResponse getProfile(Long accountId);
    AdjustBalanceResponse adjustBalance(Long accountId, AdjustBalanceRequest request);
}
