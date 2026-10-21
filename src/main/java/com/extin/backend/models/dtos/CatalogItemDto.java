package com.extin.backend.models.dtos;

import com.extin.backend.models.enums.ItemType;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Catalog item representation for public listing and admin inspection.
 * Prices are always in USD base currency; frontend displays local estimates.
 */
public record CatalogItemDto(
        @JsonProperty("id")
        UUID id,

        @JsonProperty("sku_code")
        String skuCode,

        @JsonProperty("name")
        String name,

        @JsonProperty("type")
        ItemType type,

        @JsonProperty("base_price")
        BigDecimal basePrice,

        @JsonProperty("currency")
        String currency,

        @JsonProperty("image_url")
        String imageUrl,

        @JsonProperty("description")
        String description,

        @JsonProperty("active")
        Boolean active
) {
}
