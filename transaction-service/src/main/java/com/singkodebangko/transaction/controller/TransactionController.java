package com.singkodebangko.transaction.controller;

import com.singkodebangko.transaction.dto.TransactionHistoryResponse;
import com.singkodebangko.transaction.dto.TransferRequest;
import com.singkodebangko.transaction.dto.TransferResponse;
import com.singkodebangko.transaction.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/transactions", "/api/transactions"})
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request) {
        TransferResponse response = transactionService.executeTransfer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping({"/account/{accountId}", "/history/{accountId}"})
    public ResponseEntity<List<TransactionHistoryResponse>> getHistory(@PathVariable("accountId") Long accountId) {
        List<TransactionHistoryResponse> history = transactionService.getTransactionHistory(accountId);
        return ResponseEntity.ok(history);
    }
}
