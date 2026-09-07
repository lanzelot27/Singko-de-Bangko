package com.singkodebangko.transaction.client;

import com.singkodebangko.transaction.dto.AccountBalanceResponse;
import com.singkodebangko.transaction.dto.AccountProfileResponse;
import com.singkodebangko.transaction.dto.AdjustBalanceRequest;
import com.singkodebangko.transaction.dto.AdjustBalanceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * OpenFeign declarative REST client discovering and calling 'account-service' dynamically via Eureka.
 * Fallback to account-service.url is provided for standalone local development testing.
 */
@FeignClient(name = "account-service", url = "${account-service.url:}")
public interface AccountClient {

    @GetMapping("/accounts/{accountId}/balance")
    AccountBalanceResponse getBalance(@PathVariable("accountId") Long accountId);

    @GetMapping("/accounts/{accountId}/profile")
    AccountProfileResponse getProfile(@PathVariable("accountId") Long accountId);

    @PutMapping("/accounts/{accountId}/adjust-balance")
    AdjustBalanceResponse adjustBalance(
            @PathVariable("accountId") Long accountId,
            @RequestBody AdjustBalanceRequest request
    );
}
