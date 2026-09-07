package com.singko.gateway.dto;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

public record ErrorResponse(
    String timestamp,
    int status,
    String error,
    String message,
    String path
) {
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(
            DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
            status,
            error,
            message,
            path
        );
    }
}
