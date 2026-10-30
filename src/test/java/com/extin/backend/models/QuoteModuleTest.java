package com.extin.backend.models;

import com.extin.backend.mappers.QuoteMapper;
import com.extin.backend.models.dtos.CustomerInfoDto;
import com.extin.backend.models.dtos.QuoteItemRequestDto;
import com.extin.backend.models.dtos.QuoteRequestDto;
import com.extin.backend.models.dtos.QuoteResponseDto;
import com.extin.backend.models.entities.Customer;
import com.extin.backend.models.entities.Quote;
import com.extin.backend.models.enums.QuoteStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class QuoteModuleTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final QuoteMapper quoteMapper = new QuoteMapper();

    @Test
    @DisplayName("Quote entity should correctly store customerCity, customerAddress, and documentNumber")
    void testQuoteEntityNewFields() {
        Quote quote = new Quote();
        quote.setCustomerCity("Valencia");
        quote.setCustomerAddress("Av. Bolívar Norte, Sector Camoruco");
        quote.setDocumentNumber("003877");
        quote.setStatus(QuoteStatus.INVOICED);

        assertThat(quote.getCustomerCity()).isEqualTo("Valencia");
        assertThat(quote.getCustomerAddress()).isEqualTo("Av. Bolívar Norte, Sector Camoruco");
        assertThat(quote.getDocumentNumber()).isEqualTo("003877");
        assertThat(quote.getStatus()).isEqualTo(QuoteStatus.INVOICED);
        assertThat(quote.toString()).contains("customerCity='Valencia'", "customerAddress='Av. Bolívar Norte, Sector Camoruco'", "documentNumber='003877'");
    }

    @Test
    @DisplayName("QuoteStatus enum must contain INVOICED for cash invoices converted from quotes")
    void testQuoteStatusEnumContainsInvoiced() {
        QuoteStatus[] statuses = QuoteStatus.values();
        assertThat(statuses).contains(QuoteStatus.PENDING, QuoteStatus.APPROVED, QuoteStatus.REJECTED, QuoteStatus.INVOICED);
        assertThat(QuoteStatus.valueOf("INVOICED")).isEqualTo(QuoteStatus.INVOICED);
    }

    @Test
    @DisplayName("QuoteRequestDto should deserialize customer_city and customer_address from snake_case JSON")
    void testQuoteRequestDtoDeserializationSnakeCase() throws Exception {
        String json = """
                {
                  "customer": {
                    "tax_id": "J-12345678-9",
                    "business_name": "Inversiones El Sol C.A.",
                    "email": "contacto@elsol.com"
                  },
                  "customer_city": "Caracas",
                  "customer_address": "Av. Francisco de Miranda, Torre Delta, Piso 4",
                  "target_currency": "VES",
                  "items": [
                    { "sku": "EXT-PQS-10", "quantity": 2, "type": "PRODUCT" }
                  ]
                }
                """;

        QuoteRequestDto dto = objectMapper.readValue(json, QuoteRequestDto.class);
        assertThat(dto.customerCity()).isEqualTo("Caracas");
        assertThat(dto.customerAddress()).isEqualTo("Av. Francisco de Miranda, Torre Delta, Piso 4");
        assertThat(dto.targetCurrency()).isEqualTo("VES");
        assertThat(dto.items()).hasSize(1);
    }

    @Test
    @DisplayName("QuoteRequestDto should deserialize customerCity and customerAddress from camelCase JSON")
    void testQuoteRequestDtoDeserializationCamelCase() throws Exception {
        String json = """
                {
                  "customer": {
                    "tax_id": "J-12345678-9",
                    "business_name": "Inversiones El Sol C.A.",
                    "email": "contacto@elsol.com"
                  },
                  "customerCity": "Maracaibo",
                  "customerAddress": "Calle 72 con Av. 15",
                  "target_currency": "USD",
                  "items": [
                    { "sku": "EXT-CO2-5", "quantity": 1, "type": "PRODUCT" }
                  ]
                }
                """;

        QuoteRequestDto dto = objectMapper.readValue(json, QuoteRequestDto.class);
        assertThat(dto.customerCity()).isEqualTo("Maracaibo");
        assertThat(dto.customerAddress()).isEqualTo("Calle 72 con Av. 15");
        assertThat(dto.targetCurrency()).isEqualTo("USD");
    }

    @Test
    @DisplayName("QuoteMapper should map customerCity, customerAddress, and documentNumber to QuoteResponseDto")
    void testQuoteMapperWithNewFields() {
        Customer customer = new Customer("J-12345678-9", "Test Corp", "Main St 123", "555-1234", "test@test.com");
        Quote quote = new Quote(customer, LocalDateTime.now(), QuoteStatus.INVOICED, "USD", "VES", new BigDecimal("36.50"));
        quote.setId(UUID.randomUUID());
        quote.setCustomerCity("Mérida");
        quote.setCustomerAddress("Av. Las Américas, Edif. Los Pinos");
        quote.setDocumentNumber("003877");
        quote.setSubtotal(new BigDecimal("100.00"));
        quote.setAppliesIva(true);
        quote.setTaxRate(new BigDecimal("16.00"));
        quote.setTaxAmount(new BigDecimal("16.00"));
        quote.setTotalAmount(new BigDecimal("116.00"));
        quote.setConvertedTotalAmount(new BigDecimal("4234.00"));

        QuoteResponseDto responseDto = quoteMapper.toDto(quote);

        assertThat(responseDto).isNotNull();
        assertThat(responseDto.status()).isEqualTo("INVOICED");
        assertThat(responseDto.customerCity()).isEqualTo("Mérida");
        assertThat(responseDto.customerAddress()).isEqualTo("Av. Las Américas, Edif. Los Pinos");
        assertThat(responseDto.documentNumber()).isEqualTo("003877");
        assertThat(responseDto.subtotal()).isEqualByComparingTo("100.00");
        assertThat(responseDto.appliesIva()).isTrue();
        assertThat(responseDto.taxRate()).isEqualByComparingTo("16.00");
        assertThat(responseDto.taxAmount()).isEqualByComparingTo("16.00");
        assertThat(responseDto.totalAmountUsd()).isEqualByComparingTo("116.00");
        assertThat(responseDto.totalAmountLocal()).isEqualByComparingTo("4234.00");
    }

    @Test
    @DisplayName("Quote recalculateTotals with appliesIva=true calculates taxAmount and sums to totalAmount")
    void testRecalculateTotalsWithIva() {
        Quote quote = new Quote();
        quote.setAppliesIva(true);
        quote.setTaxRate(new BigDecimal("16.00"));
        quote.setExchangeRate(new BigDecimal("45.00"));

        com.extin.backend.models.entities.CatalogItem item = new com.extin.backend.models.entities.CatalogItem("EXT-PQS-10", "Extintor PQS 10", com.extin.backend.models.enums.ItemType.PRODUCT, new BigDecimal("50.00"));
        com.extin.backend.models.entities.QuoteDetail detail = new com.extin.backend.models.entities.QuoteDetail(quote, item, 2, new BigDecimal("50.00")); // subtotal = 100.00
        quote.addDetail(detail);

        quote.recalculateTotals();

        assertThat(quote.getSubtotal()).isEqualByComparingTo("100.00");
        assertThat(quote.getAppliesIva()).isTrue();
        assertThat(quote.getTaxRate()).isEqualByComparingTo("16.00");
        assertThat(quote.getTaxAmount()).isEqualByComparingTo("16.00");
        assertThat(quote.getTotalAmount()).isEqualByComparingTo("116.00");
        assertThat(quote.getConvertedTotalAmount()).isEqualByComparingTo("5220.00"); // 116 * 45 = 5220.00
    }

    @Test
    @DisplayName("Quote recalculateTotals with appliesIva=false sets taxAmount to 0 and totalAmount equal to subtotal")
    void testRecalculateTotalsWithoutIva() {
        Quote quote = new Quote();
        quote.setAppliesIva(false);
        quote.setTaxRate(new BigDecimal("16.00"));
        quote.setExchangeRate(new BigDecimal("45.00"));

        com.extin.backend.models.entities.CatalogItem item = new com.extin.backend.models.entities.CatalogItem("REC-CO2-5", "Recarga CO2 5", com.extin.backend.models.enums.ItemType.SERVICE, new BigDecimal("25.00"));
        com.extin.backend.models.entities.QuoteDetail detail = new com.extin.backend.models.entities.QuoteDetail(quote, item, 4, new BigDecimal("25.00")); // subtotal = 100.00
        quote.addDetail(detail);

        quote.recalculateTotals();

        assertThat(quote.getSubtotal()).isEqualByComparingTo("100.00");
        assertThat(quote.getAppliesIva()).isFalse();
        assertThat(quote.getTaxAmount()).isEqualByComparingTo("0.00");
        assertThat(quote.getTotalAmount()).isEqualByComparingTo("100.00");
        assertThat(quote.getConvertedTotalAmount()).isEqualByComparingTo("4500.00"); // 100 * 45 = 4500.00
    }

    @Test
    @DisplayName("QuoteRequestDto should deserialize applies_iva and tax_rate from JSON")
    void testQuoteRequestDtoDeserializationTaxFields() throws Exception {
        String json = """
                {
                  "customer": {
                    "tax_id": "J-12345678-9",
                    "business_name": "Inversiones Industriales C.A.",
                    "email": "compras@empresa.com"
                  },
                  "applies_iva": true,
                  "tax_rate": 16.00,
                  "target_currency": "USD",
                  "items": [
                    { "sku": "EXT-PQS-10", "quantity": 1, "type": "PRODUCT" }
                  ]
                }
                """;

        QuoteRequestDto dto = objectMapper.readValue(json, QuoteRequestDto.class);
        assertThat(dto.appliesIva()).isTrue();
        assertThat(dto.taxRate()).isEqualByComparingTo("16.00");
    }
}
