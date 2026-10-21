package com.extin.backend.models.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * Customer information provided during quote creation.
 */
public record CustomerInfoDto(
        @JsonProperty("tax_id")
        @NotBlank(message = "Tax ID (RIF/CNPJ/Cédula) is required")
        String taxId,

        @JsonProperty("business_name")
        @NotBlank(message = "Business name is required")
        String businessName,

        @JsonProperty("email")
        String email
) {
}
