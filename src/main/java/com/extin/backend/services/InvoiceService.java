package com.extin.backend.services;

import com.extin.backend.exceptions.ResourceNotFoundException;
import com.extin.backend.mappers.InvoiceMapper;
import com.extin.backend.models.dtos.InvoiceResponseDto;
import com.extin.backend.models.dtos.PayAndIssueInvoiceRequestDto;
import com.extin.backend.models.entities.Invoice;
import com.extin.backend.models.entities.InvoiceDetail;
import com.extin.backend.models.entities.Quote;
import com.extin.backend.models.entities.QuoteDetail;
import com.extin.backend.models.enums.InvoiceStatus;
import com.extin.backend.models.enums.QuoteStatus;
import com.extin.backend.repositories.InvoiceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for issuing invoices from quotes.
 * Uses InvoiceMapper for transforming Invoice entities to DTO records.
 */
@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final QuoteService quoteService;
    private final InvoiceMapper invoiceMapper;

    public InvoiceService(InvoiceRepository invoiceRepository,
                          QuoteService quoteService,
                          InvoiceMapper invoiceMapper) {
        this.invoiceRepository = invoiceRepository;
        this.quoteService = quoteService;
        this.invoiceMapper = invoiceMapper;
    }

    /**
     * Issues an immutable paid invoice from a quote upon payment notification.
     * Conforms strictly to docs/api-contracts.md Section 2.1 and docs/system-spec.md Section 2.3.
     */
    @Transactional
    public InvoiceResponseDto payAndIssueInvoice(UUID quoteId, PayAndIssueInvoiceRequestDto requestDto) {
        if (quoteId == null) {
            throw new IllegalArgumentException("quote_id cannot be null");
        }
        if (requestDto == null) {
            throw new IllegalArgumentException("Request payload cannot be null");
        }

        // 1. Search quote by quote_id
        Quote quote = quoteService.getQuoteById(quoteId);

        // Check if an invoice was already issued for this quote
        if (invoiceRepository.findByQuoteId(quoteId).isPresent()
                || invoiceRepository.findByOriginQuoteId(quoteId).isPresent()) {
            throw new IllegalStateException("An invoice has already been issued for quote ID: " + quoteId);
        }

        BigDecimal exchangeRate = requestDto.exchangeRate();
        if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("exchange_rate must be greater than zero");
        }

        // 2. Exact mathematical conversion from USD base amount
        BigDecimal totalAmountUsd = quote.getTotalAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmountLocal = totalAmountUsd.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);

        // 3. Build immutable Invoice
        LocalDateTime rateDate = LocalDateTime.now();
        Invoice invoice = new Invoice();
        invoice.setQuote(quote);
        invoice.setCustomer(quote.getCustomer());
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setBaseCurrency("USD");
        invoice.setTargetCurrency(requestDto.targetCurrency());
        invoice.setExchangeRate(exchangeRate);
        invoice.setTotalAmountUsd(totalAmountUsd);
        invoice.setTotalAmountLocal(totalAmountLocal);
        invoice.setRateDate(rateDate);
        invoice.setIssueDate(rateDate);
        invoice.setPaymentReference(requestDto.paymentReference());
        invoice.setFiscalControlNumber("INV-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        // 4. Immutable copy of items to invoice_details
        if (quote.getDetails() != null) {
            for (QuoteDetail qd : quote.getDetails()) {
                InvoiceDetail detail = new InvoiceDetail();
                detail.setInvoice(invoice);
                detail.setItem(qd.getItem());
                detail.setQuantity(qd.getQuantity());

                BigDecimal unitPriceUsd = qd.getUnitPrice() != null
                        ? qd.getUnitPrice().setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                BigDecimal subtotalUsd = qd.getSubtotal() != null
                        ? qd.getSubtotal().setScale(2, RoundingMode.HALF_UP)
                        : unitPriceUsd.multiply(BigDecimal.valueOf(qd.getQuantity())).setScale(2, RoundingMode.HALF_UP);

                BigDecimal convertedUnitPrice = unitPriceUsd.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);
                BigDecimal convertedSubtotal = subtotalUsd.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);

                detail.setUnitPrice(unitPriceUsd);
                detail.setSubtotal(subtotalUsd);
                detail.setConvertedUnitPrice(convertedUnitPrice);
                detail.setConvertedSubtotal(convertedSubtotal);

                invoice.addDetail(detail);
            }
        }

        // 5. Update quote status to INVOICED
        quote.setStatus(QuoteStatus.INVOICED);

        // 6. Save invoice
        Invoice saved = invoiceRepository.save(invoice);

        // 7. Map to exact contract response using InvoiceMapper
        return invoiceMapper.toPaidInvoiceDto(saved);
    }

    /**
     * Converts an approved quote into an immutable fiscal invoice with fiscal control number.
     * Kept for backward compatibility.
     */
    @Transactional
    public InvoiceResponseDto generateInvoiceFromQuote(UUID quoteId) {
        Quote quote = quoteService.getQuoteById(quoteId);

        // Check if invoice already exists for this quote
        if (invoiceRepository.findByOriginQuoteId(quoteId).isPresent()
                || invoiceRepository.findByQuoteId(quoteId).isPresent()) {
            throw new IllegalStateException("An invoice has already been generated for quote ID: " + quoteId);
        }

        // Transition quote status to INVOICED
        quote.setStatus(QuoteStatus.INVOICED);

        // Generate unique legal fiscal control number
        String fiscalControlNumber = String.format("SENIAT-%d-%s",
                LocalDate.now().getYear(),
                UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        Invoice invoice = new Invoice();
        invoice.setFiscalControlNumber(fiscalControlNumber);
        invoice.setQuote(quote);
        invoice.setCustomer(quote.getCustomer());
        invoice.setIssueDate(LocalDateTime.now());
        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setBaseCurrency(quote.getBaseCurrency());
        invoice.setTargetCurrency(quote.getTargetCurrency());
        invoice.setExchangeRate(quote.getExchangeRate());
        invoice.setTotalAmountUsd(quote.getTotalAmount());
        invoice.setTotalAmountLocal(quote.getConvertedTotalAmount());

        if (quote.getDetails() != null) {
            for (QuoteDetail qd : quote.getDetails()) {
                InvoiceDetail id = new InvoiceDetail();
                id.setInvoice(invoice);
                id.setItem(qd.getItem());
                id.setQuantity(qd.getQuantity());
                id.setUnitPrice(qd.getUnitPrice());
                id.setSubtotal(qd.getSubtotal());
                id.setConvertedUnitPrice(qd.getConvertedUnitPrice());
                id.setConvertedSubtotal(qd.getConvertedSubtotal());

                invoice.addDetail(id);
            }
        }

        Invoice saved = invoiceRepository.save(invoice);
        return invoiceMapper.toDto(saved);
    }

    /**
     * Retrieves all invoices paginated for administrative inspection.
     */
    @Transactional(readOnly = true)
    public Page<InvoiceResponseDto> getAllInvoices(Pageable pageable) {
        return invoiceRepository.findAll(pageable).map(invoiceMapper::toPaidInvoiceDto);
    }

    /**
     * Retrieves a single invoice by its UUID.
     */
    @Transactional(readOnly = true)
    public InvoiceResponseDto getInvoiceDtoById(UUID id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with ID: " + id));
        return invoiceMapper.toPaidInvoiceDto(invoice);
    }
}
