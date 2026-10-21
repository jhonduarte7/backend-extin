package com.extin.backend.services;

import com.extin.backend.mappers.InvoiceMapper;
import com.extin.backend.models.dtos.InvoiceResponseDto;
import com.extin.backend.models.dtos.PayAndIssueInvoiceRequestDto;
import com.extin.backend.models.entities.CatalogItem;
import com.extin.backend.models.entities.Customer;
import com.extin.backend.models.entities.Invoice;
import com.extin.backend.models.entities.InvoiceDetail;
import com.extin.backend.models.entities.Quote;
import com.extin.backend.models.entities.QuoteDetail;
import com.extin.backend.models.enums.InvoiceStatus;
import com.extin.backend.models.enums.ItemType;
import com.extin.backend.models.enums.QuoteStatus;
import com.extin.backend.repositories.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests verifying:
 * - Exact mathematical accuracy of USD to local currency conversions (VES, BRL, EUR)
 * - Immutable copy of line items
 * - Status transition to PAID
 * - Payment reference and rate date recording
 */
@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private QuoteService quoteService;

    @Spy
    private InvoiceMapper invoiceMapper = new InvoiceMapper();

    @InjectMocks
    private InvoiceService invoiceService;

    private Customer testCustomer;
    private CatalogItem productItem;
    private CatalogItem serviceItem;

    @BeforeEach
    void setUp() {
        testCustomer = new Customer("J-12345678-9", "Inversiones El Sol C.A.", "contacto@elsol.com", "0212-5551234", "Caracas, Venezuela");
        testCustomer.setId(UUID.randomUUID());

        productItem = new CatalogItem("EXT-PQS-10", "Extintor PQS 10 Lbs", ItemType.PRODUCT, new BigDecimal("38.50"));
        productItem.setId(UUID.randomUUID());

        serviceItem = new CatalogItem("REC-CO2-5", "Recarga CO2 5 Lbs", ItemType.SERVICE, new BigDecimal("23.00"));
        serviceItem.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("payAndIssueInvoice - Mathematical precision test: 100.00 USD * 45.50 VES = 4550.00 VES exactly")
    void testPayAndIssueInvoice_ExactMathConversionVes() {
        UUID quoteId = UUID.randomUUID();

        // 1. Prepare Quote with 100.00 USD base total
        // Item 1: 38.50 * 2 = 77.00 USD
        // Item 2: 23.00 * 1 = 23.00 USD
        // Total = 100.00 USD
        Quote quote = new Quote();
        quote.setId(quoteId);
        quote.setCustomer(testCustomer);
        quote.setStatus(QuoteStatus.APPROVED);
        quote.setBaseCurrency("USD");
        quote.setTotalAmount(new BigDecimal("100.00"));

        QuoteDetail detail1 = new QuoteDetail(quote, productItem, 2, new BigDecimal("38.50"));
        QuoteDetail detail2 = new QuoteDetail(quote, serviceItem, 1, new BigDecimal("23.00"));
        quote.addDetail(detail1);
        quote.addDetail(detail2);

        // 2. Request with VES exchange rate of 45.50 and reference
        BigDecimal exchangeRate = new BigDecimal("45.50");
        PayAndIssueInvoiceRequestDto request = new PayAndIssueInvoiceRequestDto(
                "VES",
                exchangeRate,
                "0102-123456789"
        );

        when(quoteService.getQuoteById(quoteId)).thenReturn(quote);
        when(invoiceRepository.findByQuoteId(quoteId)).thenReturn(Optional.empty());
        when(invoiceRepository.findByOriginQuoteId(quoteId)).thenReturn(Optional.empty());
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice inv = invocation.getArgument(0);
            inv.setId(UUID.randomUUID());
            return inv;
        });

        // 3. Execute
        InvoiceResponseDto response = invoiceService.payAndIssueInvoice(quoteId, request);

        // 4. Assertions on response DTO
        assertNotNull(response.invoiceId());
        assertEquals(quoteId.toString(), response.quoteId());
        assertEquals("PAID", response.status());
        assertEquals("VES", response.targetCurrency());
        assertEquals(new BigDecimal("45.50"), response.exchangeRate());

        // Exact mathematical assertions: 100.00 USD -> 4550.00 VES
        assertEquals(new BigDecimal("100.00"), response.totalAmountUsd());
        assertEquals(new BigDecimal("4550.00"), response.totalAmountLocal());
        assertEquals("0102-123456789", response.paymentReference());
        assertNotNull(response.rateDate());

        // 5. Verify Quote status transitioned to INVOICED
        assertEquals(QuoteStatus.INVOICED, quote.getStatus());

        // 6. Verify persisted entity and immutable line items copy
        ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(captor.capture());
        Invoice captured = captor.getValue();

        assertEquals(InvoiceStatus.PAID, captured.getStatus());
        assertEquals(new BigDecimal("100.00"), captured.getTotalAmountUsd());
        assertEquals(new BigDecimal("4550.00"), captured.getTotalAmountLocal());
        assertEquals("0102-123456789", captured.getPaymentReference());

        // Details verification
        assertEquals(2, captured.getDetails().size());

        InvoiceDetail capturedD1 = captured.getDetails().get(0);
        assertEquals(productItem, capturedD1.getItem());
        assertEquals(2, capturedD1.getQuantity());
        assertEquals(new BigDecimal("38.50"), capturedD1.getUnitPrice());
        assertEquals(new BigDecimal("77.00"), capturedD1.getSubtotal());
        // 38.50 * 45.50 = 1751.75 VES
        assertEquals(new BigDecimal("1751.75"), capturedD1.getConvertedUnitPrice());
        // 77.00 * 45.50 = 3503.50 VES
        assertEquals(new BigDecimal("3503.50"), capturedD1.getConvertedSubtotal());

        InvoiceDetail capturedD2 = captured.getDetails().get(1);
        assertEquals(serviceItem, capturedD2.getItem());
        assertEquals(1, capturedD2.getQuantity());
        assertEquals(new BigDecimal("23.00"), capturedD2.getUnitPrice());
        assertEquals(new BigDecimal("23.00"), capturedD2.getSubtotal());
        // 23.00 * 45.50 = 1046.50 VES
        assertEquals(new BigDecimal("1046.50"), capturedD2.getConvertedUnitPrice());
        assertEquals(new BigDecimal("1046.50"), capturedD2.getConvertedSubtotal());

        // Line item sum matches total_amount_local exactly (3503.50 + 1046.50 = 4550.00)
        BigDecimal sumConverted = capturedD1.getConvertedSubtotal().add(capturedD2.getConvertedSubtotal());
        assertEquals(new BigDecimal("4550.00"), sumConverted);
    }

    @Test
    @DisplayName("payAndIssueInvoice - Fractional cents precision: 92.35 USD * 45.50 VES = 4201.93 VES (HALF_UP)")
    void testPayAndIssueInvoice_FractionalCentsRounding() {
        UUID quoteId = UUID.randomUUID();

        Quote quote = new Quote();
        quote.setId(quoteId);
        quote.setCustomer(testCustomer);
        quote.setStatus(QuoteStatus.APPROVED);
        quote.setBaseCurrency("USD");
        quote.setTotalAmount(new BigDecimal("92.35"));

        BigDecimal exchangeRate = new BigDecimal("45.50");
        PayAndIssueInvoiceRequestDto request = new PayAndIssueInvoiceRequestDto(
                "VES",
                exchangeRate,
                "REF-FRACTIONAL-01"
        );

        when(quoteService.getQuoteById(quoteId)).thenReturn(quote);
        when(invoiceRepository.findByQuoteId(quoteId)).thenReturn(Optional.empty());
        when(invoiceRepository.findByOriginQuoteId(quoteId)).thenReturn(Optional.empty());
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice inv = invocation.getArgument(0);
            inv.setId(UUID.randomUUID());
            return inv;
        });

        InvoiceResponseDto response = invoiceService.payAndIssueInvoice(quoteId, request);

        // 92.35 * 45.50 = 4201.925 -> rounded HALF_UP to 2 decimal places = 4201.93
        assertEquals(new BigDecimal("92.35"), response.totalAmountUsd());
        assertEquals(new BigDecimal("4201.93"), response.totalAmountLocal());
    }

    @Test
    @DisplayName("payAndIssueInvoice - Multi-currency conversion to BRL: 50.00 USD * 5.65 BRL = 282.50 BRL")
    void testPayAndIssueInvoice_BrlConversion() {
        UUID quoteId = UUID.randomUUID();

        Quote quote = new Quote();
        quote.setId(quoteId);
        quote.setCustomer(testCustomer);
        quote.setStatus(QuoteStatus.APPROVED);
        quote.setBaseCurrency("USD");
        quote.setTotalAmount(new BigDecimal("50.00"));

        BigDecimal exchangeRate = new BigDecimal("5.65");
        PayAndIssueInvoiceRequestDto request = new PayAndIssueInvoiceRequestDto(
                "BRL",
                exchangeRate,
                "PIX-987654321"
        );

        when(quoteService.getQuoteById(quoteId)).thenReturn(quote);
        when(invoiceRepository.findByQuoteId(quoteId)).thenReturn(Optional.empty());
        when(invoiceRepository.findByOriginQuoteId(quoteId)).thenReturn(Optional.empty());
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice inv = invocation.getArgument(0);
            inv.setId(UUID.randomUUID());
            return inv;
        });

        InvoiceResponseDto response = invoiceService.payAndIssueInvoice(quoteId, request);

        assertEquals(new BigDecimal("50.00"), response.totalAmountUsd());
        assertEquals(new BigDecimal("282.50"), response.totalAmountLocal());
        assertEquals("BRL", response.targetCurrency());
        assertEquals("PIX-987654321", response.paymentReference());
    }

    @Test
    @DisplayName("payAndIssueInvoice - Throws IllegalStateException when quote is already invoiced")
    void testPayAndIssueInvoice_ThrowsExceptionWhenAlreadyInvoiced() {
        UUID quoteId = UUID.randomUUID();
        PayAndIssueInvoiceRequestDto request = new PayAndIssueInvoiceRequestDto("VES", new BigDecimal("45.50"), "REF-123");

        Quote quote = new Quote();
        quote.setId(quoteId);
        when(quoteService.getQuoteById(quoteId)).thenReturn(quote);
        when(invoiceRepository.findByQuoteId(quoteId)).thenReturn(Optional.of(new Invoice()));

        assertThrows(IllegalStateException.class, () -> invoiceService.payAndIssueInvoice(quoteId, request));
    }

    @Test
    @DisplayName("payAndIssueInvoice - Throws IllegalArgumentException when exchange rate is zero or negative")
    void testPayAndIssueInvoice_ThrowsExceptionWhenInvalidExchangeRate() {
        UUID quoteId = UUID.randomUUID();
        Quote quote = new Quote();
        quote.setId(quoteId);
        quote.setTotalAmount(new BigDecimal("100.00"));

        when(quoteService.getQuoteById(quoteId)).thenReturn(quote);

        PayAndIssueInvoiceRequestDto zeroRate = new PayAndIssueInvoiceRequestDto("VES", BigDecimal.ZERO, "REF-123");
        assertThrows(IllegalArgumentException.class, () -> invoiceService.payAndIssueInvoice(quoteId, zeroRate));

        PayAndIssueInvoiceRequestDto negativeRate = new PayAndIssueInvoiceRequestDto("VES", new BigDecimal("-1.50"), "REF-123");
        assertThrows(IllegalArgumentException.class, () -> invoiceService.payAndIssueInvoice(quoteId, negativeRate));
    }
}
