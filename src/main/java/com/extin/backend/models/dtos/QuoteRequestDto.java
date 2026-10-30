package com.extin.backend.models.dtos;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request payload for creating a multi-currency quote from the public web portal.
 * Receives customer information, optional city and address, target currency, tax options, and line items.
 */
public record QuoteRequestDto(
        @JsonProperty("customer")
        @NotNull(message = "Customer information is required")
        @Valid
        CustomerInfoDto customer,

        @JsonProperty("customer_city")
        @JsonAlias({"customerCity", "customer_city", "city"})
        String customerCity,

        @JsonProperty("customer_address")
        @JsonAlias({"customerAddress", "customer_address", "address"})
        String customerAddress,

        @JsonProperty("target_currency")
        String targetCurrency,

        @JsonProperty("applies_iva")
        @JsonAlias({"appliesIva", "applies_iva"})
        Boolean appliesIva,

        @JsonProperty("tax_rate")
        @JsonAlias({"taxRate", "tax_rate"})
        BigDecimal taxRate,

        @JsonProperty("items")
        @NotEmpty(message = "At least one item is required")
        List<@Valid QuoteItemRequestDto> items
) {
    /**
     * Backward-compatible convenience constructors.
     */
    public QuoteRequestDto(CustomerInfoDto customer, String targetCurrency, List<QuoteItemRequestDto> items) {
        this(customer, null, null, targetCurrency, null, null, items);
    }

    public QuoteRequestDto(CustomerInfoDto customer, String customerCity, String customerAddress, String targetCurrency, List<QuoteItemRequestDto> items) {
        this(customer, customerCity, customerAddress, targetCurrency, null, null, items);
    }
}
