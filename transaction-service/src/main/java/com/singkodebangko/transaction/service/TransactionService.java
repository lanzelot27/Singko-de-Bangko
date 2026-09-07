package com.singkodebangko.transaction.service;

import com.singkodebangko.transaction.dto.TransactionHistoryResponse;
import com.singkodebangko.transaction.dto.TransferRequest;
import com.singkodebangko.transaction.dto.TransferResponse;

import java.util.List;

public interface TransactionService {
    TransferResponse executeTransfer(TransferRequest request);
    List<TransactionHistoryResponse> getTransactionHistory(Long accountId);
}
