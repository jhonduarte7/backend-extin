package com.extin.backend.models.dtos;

import java.time.LocalDateTime;

/**
 * Standardized immutable error response record returned by the GlobalExceptionHandler.
 * Conforms to REST API best practices, encapsulating error code, message, timestamp, and requested path.
 */
public record ErrorResponseDto(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path
) {}
