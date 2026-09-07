package com.singkodebangko.transaction.exception;

public class TransferProcessingException extends RuntimeException {
    public TransferProcessingException(String message) {
        super(message);
    }

    public TransferProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
