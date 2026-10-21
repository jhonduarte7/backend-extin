package com.extin.backend.models.dtos;

import jakarta.validation.constraints.NotBlank;

/**
 * Immutable DTO record for user login request credentials.
 */
public record LoginRequestDto(
        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Password is required")
        String password
) {}
