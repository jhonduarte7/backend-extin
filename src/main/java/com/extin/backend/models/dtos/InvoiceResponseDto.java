package com.extin.backend.models.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for Invoice operations.
 * Exactly matches docs/api-contracts.md Section 2.1 while supporting backward-compatible fields.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record InvoiceResponseDto(
        @JsonProperty("invoice_id")
        String invoiceId,

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

        @JsonProperty("rate_date")
        LocalDateTime rateDate,

        @JsonProperty("payment_reference")
        String paymentReference,

        // Optional / legacy fields for from-quote endpoint
        @JsonProperty("fiscal_control_number")
        String fiscalControlNumber,

        @JsonProperty("origin_quote_id")
        String originQuoteId,

        @JsonProperty("customer_name")
        String customerName,

        @JsonProperty("customer_tax_id")
        String customerTaxId,

        @JsonProperty("issue_date")
        LocalDateTime issueDate,

        @JsonProperty("base_currency")
        String baseCurrency,

        @JsonProperty("message")
        String message
) {
    /**
     * Factory method for Paid Invoice with tax breakdown.
     */
    public static InvoiceResponseDto forPaidInvoice(
            String invoiceId,
            String quoteId,
            String status,
            String targetCurrency,
            BigDecimal exchangeRate,
            BigDecimal totalAmountUsd,
            BigDecimal totalAmountLocal,
            LocalDateTime rateDate,
            String paymentReference,
            BigDecimal subtotal,
            Boolean appliesIva,
            BigDecimal taxRate,
            BigDecimal taxAmount
    ) {
        return new InvoiceResponseDto(
                invoiceId,
                quoteId,
                status,
                targetCurrency,
                exchangeRate,
                subtotal,
                appliesIva,
                taxRate,
                taxAmount,
                totalAmountUsd,
                totalAmountLocal,
                rateDate,
                paymentReference,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    /**
     * Factory method for Paid Invoice (backward compatible).
     */
    public static InvoiceResponseDto forPaidInvoice(
            String invoiceId,
            String quoteId,
            String status,
            String targetCurrency,
            BigDecimal exchangeRate,
            BigDecimal totalAmountUsd,
            BigDecimal totalAmountLocal,
            LocalDateTime rateDate,
            String paymentReference
    ) {
        return forPaidInvoice(
                invoiceId,
                quoteId,
                status,
                targetCurrency,
                exchangeRate,
                totalAmountUsd,
                totalAmountLocal,
                rateDate,
                paymentReference,
                totalAmountUsd,
                Boolean.FALSE,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
    }
}
