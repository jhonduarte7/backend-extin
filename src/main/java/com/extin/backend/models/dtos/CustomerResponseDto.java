package com.extin.backend.models.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Customer representation for administrative management and search responses.
 */
public record CustomerResponseDto(
        @JsonProperty("id")
        UUID id,

        @JsonProperty("tax_id")
        String taxId,

        @JsonProperty("business_name")
        String businessName,

        @JsonProperty("address")
        String address,

        @JsonProperty("phone")
        String phone,

        @JsonProperty("email")
        String email
) {
}
