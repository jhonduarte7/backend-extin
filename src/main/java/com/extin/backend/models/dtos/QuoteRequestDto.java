package com.extin.backend.models.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request payload for creating a multi-currency quote from the public web portal.
 */
public record QuoteRequestDto(
        @JsonProperty("customer")
        @NotNull(message = "Customer information is required")
        @Valid
        CustomerInfoDto customer,

        @JsonProperty("target_currency")
        String targetCurrency,

        @JsonProperty("items")
        @NotEmpty(message = "At least one item is required")
        List<@Valid QuoteItemRequestDto> items
) {
}
