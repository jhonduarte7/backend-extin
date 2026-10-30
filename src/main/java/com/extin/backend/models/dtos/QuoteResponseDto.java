package com.extin.backend.models.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response payload returned after generating or querying a multi-currency quote.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record QuoteResponseDto(
        @JsonProperty("quote_id")
        String quoteId,

        @JsonProperty("status")
        String status,

        @JsonProperty("target_currency")
        String targetCurrency,

        @JsonProperty("exchange_rate")
        BigDecimal exchangeRate,

        @JsonProperty("subtotal")
        BigDecimal subtotal,

        @JsonProperty("applies_iva")
        Boolean appliesIva,

        @JsonProperty("tax_rate")
        BigDecimal taxRate,

        @JsonProperty("tax_amount")
        BigDecimal taxAmount,

        @JsonProperty("total_amount_usd")
        BigDecimal totalAmountUsd,

        @JsonProperty("total_amount_local")
        BigDecimal totalAmountLocal,

        @JsonProperty("customer_city")
        String customerCity,

        @JsonProperty("customer_address")
        String customerAddress,

        @JsonProperty("document_number")
        String documentNumber,

        @JsonProperty("message")
        String message,

        @JsonProperty("customer_name")
        String customerName,

        @JsonProperty("customer_tax_id")
        String customerTaxId,

        @JsonProperty("customer_phone")
        String customerPhone,

        @JsonProperty("customer_email")
        String customerEmail,

        @JsonProperty("issue_date")
        LocalDateTime issueDate,

        @JsonProperty("items")
        List<QuoteItemResponseDto> items
) {
    /**
     * Backward-compatible 14-arg constructor.
     */
    public QuoteResponseDto(String quoteId, String status, String targetCurrency,
                            BigDecimal exchangeRate, BigDecimal subtotal, Boolean appliesIva,
                            BigDecimal taxRate, BigDecimal taxAmount, BigDecimal totalAmountUsd,
                            BigDecimal totalAmountLocal, String customerCity, String customerAddress,
                            String documentNumber, String message) {
        this(quoteId, status, targetCurrency, exchangeRate, subtotal, appliesIva, taxRate, taxAmount, totalAmountUsd,
                totalAmountLocal, customerCity, customerAddress, documentNumber, message, null, null, null, null, null, null);
    }

    /**
     * Backward-compatible convenience constructor with tax fields as null/defaults.
     */
    public QuoteResponseDto(String quoteId, String status, String targetCurrency,
                            BigDecimal exchangeRate, BigDecimal totalAmountUsd,
                            BigDecimal totalAmountLocal, String message) {
        this(quoteId, status, targetCurrency, exchangeRate, totalAmountUsd, false, BigDecimal.ZERO, BigDecimal.ZERO, totalAmountUsd, totalAmountLocal,
                null, null, null, message, null, null, null, null, null, null);
    }
}
