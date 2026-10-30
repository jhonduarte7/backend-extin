package com.extin.backend.models.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Line item DTO representing a product or service inside a Quote.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record QuoteItemResponseDto(
        @JsonProperty("sku")
        String sku,

        @JsonProperty("name")
        String name,

        @JsonProperty("type")
        String type,

        @JsonProperty("quantity")
        Integer quantity,

        @JsonProperty("unit_price")
        BigDecimal unitPrice,

        @JsonProperty("subtotal")
        BigDecimal subtotal,

        @JsonProperty("converted_unit_price")
        BigDecimal convertedUnitPrice,

        @JsonProperty("converted_subtotal")
        BigDecimal convertedSubtotal
) {}
