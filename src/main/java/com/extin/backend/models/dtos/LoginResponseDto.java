package com.extin.backend.models.dtos;

/**
 * Immutable DTO record returned after successful authentication.
 * Note: Does NOT return the JWT token in the body as per strict security rules (token is in HttpOnly cookie).
 */
public record LoginResponseDto(
        String message,
        String username,
        String role
) {}
