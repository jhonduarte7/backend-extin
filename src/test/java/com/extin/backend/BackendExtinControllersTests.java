package com.extin.backend;

import com.extin.backend.models.dtos.CustomerInfoDto;
import com.extin.backend.models.dtos.PayAndIssueInvoiceRequestDto;
import com.extin.backend.models.dtos.QuoteItemRequestDto;
import com.extin.backend.models.dtos.QuoteRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.ai.vectorstore.VectorStore;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprehensive integration tests verifying REST controller endpoints:
 * - Public quote creation with multi-currency conversions (VES, BRL, USD)
 * - Public catalog pagination and searching
 * - Admin invoice settlement and pay-and-issue lifecycle
 * - Admin customer search and quotes listing
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
class BackendExtinControllersTests {

    @MockitoBean
    private VectorStore vectorStore;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/public/quotes - Should create quote with VES exchange rate multiplication")
    void testCreateQuoteVes() throws Exception {
        CustomerInfoDto customer = new CustomerInfoDto("J-12345678-9", "Inversiones El Sol C.A.", "contacto@elsol.com");
        List<QuoteItemRequestDto> items = List.of(
                new QuoteItemRequestDto("EXT-PQS-10", 2, "PRODUCT"),
                new QuoteItemRequestDto("REC-CO2-5", 1, "SERVICE")
        );
        QuoteRequestDto request = new QuoteRequestDto(customer, "VES", items);

        mockMvc.perform(post("/api/public/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quote_id", notNullValue()))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.target_currency", is("VES")))
                .andExpect(jsonPath("$.exchange_rate", is(36.50)))
                // EXT-PQS-10 is $38.50 * 2 = $77.00; REC-CO2-5 is $15.00 * 1 = $15.00; Total USD = 92.00
                .andExpect(jsonPath("$.total_amount_usd", is(92.00)))
                // Total VES = 92.00 * 36.50 = 3358.00
                .andExpect(jsonPath("$.total_amount_local", is(3358.00)))
                .andExpect(jsonPath("$.message", is("Quote generated successfully.")));
    }

    @Test
    @DisplayName("POST /api/public/quotes - Should create quote with BRL exchange rate multiplication")
    void testCreateQuoteBrl() throws Exception {
        CustomerInfoDto customer = new CustomerInfoDto("12.345.678/0001-90", "Indústria Metalúrgica Paulistana Ltda", "contato@metal.com.br");
        List<QuoteItemRequestDto> items = List.of(
                new QuoteItemRequestDto("EXT-PQS-5", 2, "PRODUCT")
        );
        QuoteRequestDto request = new QuoteRequestDto(customer, "BRL", items);

        mockMvc.perform(post("/api/public/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.target_currency", is("BRL")))
                .andExpect(jsonPath("$.exchange_rate", is(5.50)))
                // EXT-PQS-5 is $25.00 * 2 = $50.00
                .andExpect(jsonPath("$.total_amount_usd", is(50.00)))
                // Total BRL = 50.00 * 5.50 = 275.00
                .andExpect(jsonPath("$.total_amount_local", is(275.00)));
    }

    @Test
    @DisplayName("POST /api/public/quotes - Should create quote with customer_city and customer_address")
    void testCreateQuoteWithCityAndAddress() throws Exception {
        CustomerInfoDto customer = new CustomerInfoDto("J-77889900-1", "Comercializadora El Ávila C.A.", "avila@test.com");
        List<QuoteItemRequestDto> items = List.of(
                new QuoteItemRequestDto("EXT-PQS-10", 1, "PRODUCT")
        );
        QuoteRequestDto request = new QuoteRequestDto(
                customer,
                "Caracas",
                "Av. Principal de Los Ruices, Edif. Avila, Piso 2",
                "USD",
                items
        );

        mockMvc.perform(post("/api/public/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quote_id", notNullValue()))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.customer_city", is("Caracas")))
                .andExpect(jsonPath("$.customer_address", is("Av. Principal de Los Ruices, Edif. Avila, Piso 2")))
                .andExpect(jsonPath("$.total_amount_usd", is(38.50)))
                .andExpect(jsonPath("$.message", is("Quote generated successfully.")));
    }

    @Test
    @DisplayName("GET /api/public/catalog - Should return paginated items with default size 5")
    void testGetCatalogPagination() throws Exception {
        mockMvc.perform(get("/api/public/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.size", is(5)))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(20)))
                .andExpect(jsonPath("$.content[0].sku_code", notNullValue()))
                .andExpect(jsonPath("$.content[0].base_price", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/admin/customers - Should return paginated customers with default size 5")
    void testGetCustomersPagination() throws Exception {
        mockMvc.perform(get("/api/admin/customers").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.size", is(5)))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(50)));
    }

    @Test
    @DisplayName("POST /api/admin/invoices/from-quote/{quote_id} - Should convert quote into fiscal invoice")
    void testGenerateInvoiceFromQuote() throws Exception {
        // 1. Create a quote first
        CustomerInfoDto customer = new CustomerInfoDto("J-31567890-2", "Distribuidora Los Andes S.A.", "losandes@test.com");
        QuoteRequestDto request = new QuoteRequestDto(customer, "USD", List.of(
                new QuoteItemRequestDto("EXT-PQS-10", 1, "PRODUCT")
        ));

        MvcResult quoteResult = mockMvc.perform(post("/api/public/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(quoteResult.getResponse().getContentAsString());
        String quoteId = jsonNode.get("quote_id").asString();

        // 2. Convert to invoice
        mockMvc.perform(post("/api/admin/invoices/from-quote/" + quoteId).with(user("admin").roles("ADMIN")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoice_id", notNullValue()))
                .andExpect(jsonPath("$.fiscal_control_number", notNullValue()))
                .andExpect(jsonPath("$.origin_quote_id", is(quoteId)))
                .andExpect(jsonPath("$.status", is("ISSUED")))
                .andExpect(jsonPath("$.message", is("Invoice generated successfully from quote.")));
    }

    @Test
    @DisplayName("POST /api/admin/invoices/pay-and-issue/{quote_id} - Should issue paid invoice with exact local amount")
    void testPayAndIssueInvoice() throws Exception {
        // 1. Create a quote first
        CustomerInfoDto customer = new CustomerInfoDto("J-99887766-1", "Servicios Petroleros del Zulia C.A.", "contacto@zulia.com");
        QuoteRequestDto quoteRequest = new QuoteRequestDto(customer, "USD", List.of(
                new QuoteItemRequestDto("EXT-PQS-10", 2, "PRODUCT") // 38.50 * 2 = 77.00 USD
        ));

        MvcResult quoteResult = mockMvc.perform(post("/api/public/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(quoteRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(quoteResult.getResponse().getContentAsString());
        String quoteId = jsonNode.get("quote_id").asString();

        // 2. Pay and issue invoice with VES exchange rate of 45.50
        // 77.00 USD * 45.50 = 3503.50 VES
        PayAndIssueInvoiceRequestDto payRequest = new PayAndIssueInvoiceRequestDto(
                "VES",
                new BigDecimal("45.50"),
                "0102-99887766"
        );

        mockMvc.perform(post("/api/admin/invoices/pay-and-issue/" + quoteId)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoice_id", notNullValue()))
                .andExpect(jsonPath("$.quote_id", is(quoteId)))
                .andExpect(jsonPath("$.status", is("PAID")))
                .andExpect(jsonPath("$.target_currency", is("VES")))
                .andExpect(jsonPath("$.exchange_rate", is(45.50)))
                .andExpect(jsonPath("$.total_amount_usd", is(77.00)))
                .andExpect(jsonPath("$.total_amount_local", is(3503.50)))
                .andExpect(jsonPath("$.rate_date", notNullValue()))
                .andExpect(jsonPath("$.payment_reference", is("0102-99887766")));
    }

    @Test
    @DisplayName("GET /api/admin/invoices - Should return paginated invoices with default size 10")
    void testGetInvoicesPagination() throws Exception {
        mockMvc.perform(get("/api/admin/invoices").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(3)));
    }

    @Test
    @DisplayName("GET /api/admin/quotes - Should return paginated quotes with default size 10")
    void testGetQuotesPagination() throws Exception {
        mockMvc.perform(get("/api/admin/quotes").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(3)));
    }

    @Test
    @DisplayName("POST /api/admin/chat - Should execute MCP tools and return formatted response")
    void testAdminChat() throws Exception {
        String chatPayload = "{\"prompt\": \"¿Cuáles son los clientes que tienen extintores vencidos este mes?\"}";

        mockMvc.perform(post("/api/admin/chat")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(chatPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply", notNullValue()))
                .andExpect(jsonPath("$.tools_used", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.tools_used[0]", is("tool_sql_customer_history")));
    }
}
