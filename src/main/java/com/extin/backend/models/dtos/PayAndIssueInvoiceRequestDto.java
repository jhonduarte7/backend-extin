package com.extin.backend.models.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request DTO for issuing a paid invoice from an existing quote.
 * Strictly conforms to docs/api-contracts.md Section 2.1 (POST /api/admin/invoices/pay-and-issue/{quote_id}).
 */
public record PayAndIssueInvoiceRequestDto(
        @NotBlank(message = "target_currency is required")
        @JsonProperty("target_currency")
        String targetCurrency,

        @NotNull(message = "exchange_rate is required")
        @DecimalMin(value = "0.0001", message = "exchange_rate must be greater than zero")
        @JsonProperty("exchange_rate")
        BigDecimal exchangeRate,

        @NotBlank(message = "payment_reference is required")
        @JsonProperty("payment_reference")
        String paymentReference
) {
}
