package com.extin.backend.models.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Line item requested in the quote shopping cart.
 */
public record QuoteItemRequestDto(
        @JsonProperty("sku")
        @NotBlank(message = "SKU is required")
        String sku,

        @JsonProperty("quantity")
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,

        @JsonProperty("type")
        String type
) {
}
