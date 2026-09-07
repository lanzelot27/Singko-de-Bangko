package com.singkodebangko.account.controller;

import com.singkodebangko.account.dto.AccountBalanceResponse;
import com.singkodebangko.account.dto.AccountProfileResponse;
import com.singkodebangko.account.dto.AdjustBalanceRequest;
import com.singkodebangko.account.dto.AdjustBalanceResponse;
import com.singkodebangko.account.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/accounts", "/api/accounts"})
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<AccountBalanceResponse> getBalance(@PathVariable("accountId") Long accountId) {
        AccountBalanceResponse response = accountService.getBalance(accountId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accountId}/profile")
    public ResponseEntity<AccountProfileResponse> getProfile(@PathVariable("accountId") Long accountId) {
        AccountProfileResponse response = accountService.getProfile(accountId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{accountId}/adjust-balance")
    public ResponseEntity<AdjustBalanceResponse> adjustBalance(
            @PathVariable("accountId") Long accountId,
            @Valid @RequestBody AdjustBalanceRequest request) {
        AdjustBalanceResponse response = accountService.adjustBalance(accountId, request);
        return ResponseEntity.ok(response);
    }
}
