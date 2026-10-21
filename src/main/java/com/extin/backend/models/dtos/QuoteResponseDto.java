package com.extin.backend.models.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Response payload returned after generating a multi-currency quote.
 */
public record QuoteResponseDto(
        @JsonProperty("quote_id")
        String quoteId,

        @JsonProperty("status")
        String status,

        @JsonProperty("target_currency")
        String targetCurrency,

        @JsonProperty("exchange_rate")
        BigDecimal exchangeRate,

        @JsonProperty("total_amount_usd")
        BigDecimal totalAmountUsd,

        @JsonProperty("total_amount_local")
        BigDecimal totalAmountLocal,

        @JsonProperty("message")
        String message
) {
}
