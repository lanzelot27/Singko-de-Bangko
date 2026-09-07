package com.singkodebangko.account.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;

/**
 * Standard unified error response DTO record.
 */
public record ErrorResponse(
        @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
    public ErrorResponse(int status, String error, String message, String path) {
        this(Instant.now(), status, error, message, path);
    }
}
