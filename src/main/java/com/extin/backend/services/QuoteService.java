package com.extin.backend.services;

import com.extin.backend.exceptions.ResourceNotFoundException;
import com.extin.backend.mappers.QuoteMapper;
import com.extin.backend.models.dtos.QuoteItemRequestDto;
import com.extin.backend.models.dtos.QuoteRequestDto;
import com.extin.backend.models.dtos.QuoteResponseDto;
import com.extin.backend.models.entities.CatalogItem;
import com.extin.backend.models.entities.Customer;
import com.extin.backend.models.entities.Quote;
import com.extin.backend.models.entities.QuoteDetail;
import com.extin.backend.models.enums.QuoteStatus;
import com.extin.backend.repositories.QuoteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service orchestrating commercial quotes and multi-currency exchange rate conversions.
 * Uses QuoteMapper for Entity to DTO transformations.
 */
@Service
public class QuoteService {

    private final QuoteRepository quoteRepository;
    private final CustomerService customerService;
    private final CatalogService catalogService;
    private final CurrencyExchangeService currencyExchangeService;
    private final QuoteMapper quoteMapper;

    public QuoteService(QuoteRepository quoteRepository,
                        CustomerService customerService,
                        CatalogService catalogService,
                        CurrencyExchangeService currencyExchangeService,
                        QuoteMapper quoteMapper) {
        this.quoteRepository = quoteRepository;
        this.customerService = customerService;
        this.catalogService = catalogService;
        this.currencyExchangeService = currencyExchangeService;
        this.quoteMapper = quoteMapper;
    }

    /**
     * Generates a multi-currency quote from shopping cart items.
     * Multiplies base USD item prices by the exchange rate when BRL, VES, or another currency is requested.
     */
    @Transactional
    public QuoteResponseDto createQuote(QuoteRequestDto requestDto) {
        // 1. Resolve customer
        Customer customer = customerService.findOrCreateCustomer(requestDto.customer());

        // 2. Resolve currency and exchange rate
        String targetCurrency = currencyExchangeService.normalizeCurrency(requestDto.targetCurrency());
        BigDecimal exchangeRate = currencyExchangeService.getExchangeRate(targetCurrency);

        // 3. Instantiate Quote entity
        Quote quote = new Quote();
        quote.setCustomer(customer);
        quote.setIssueDate(LocalDateTime.now());
        quote.setStatus(QuoteStatus.PENDING);
        quote.setBaseCurrency(CurrencyExchangeService.CURRENCY_USD);
        quote.setTargetCurrency(targetCurrency);
        quote.setExchangeRate(exchangeRate);

        BigDecimal totalUsd = BigDecimal.ZERO;

        // 4. Process line items
        for (QuoteItemRequestDto itemDto : requestDto.items()) {
            CatalogItem catalogItem = catalogService.getEntityBySku(itemDto.sku().trim());

            int quantity = itemDto.quantity();
            BigDecimal unitPriceUsd = catalogItem.getBasePrice().setScale(2, RoundingMode.HALF_UP);
            BigDecimal subtotalUsd = unitPriceUsd.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);

            // Multi-currency converted amounts
            BigDecimal convertedUnitPrice = unitPriceUsd.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal convertedSubtotal = subtotalUsd.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);

            QuoteDetail detail = new QuoteDetail();
            detail.setQuote(quote);
            detail.setItem(catalogItem);
            detail.setQuantity(quantity);
            detail.setUnitPrice(unitPriceUsd);
            detail.setSubtotal(subtotalUsd);
            detail.setConvertedUnitPrice(convertedUnitPrice);
            detail.setConvertedSubtotal(convertedSubtotal);

            quote.addDetail(detail);
            totalUsd = totalUsd.add(subtotalUsd);
        }

        // 5. Calculate final totals
        BigDecimal totalLocal = totalUsd.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);
        quote.setTotalAmount(totalUsd);
        quote.setConvertedTotalAmount(totalLocal);

        // 6. Persist quote
        Quote savedQuote = quoteRepository.save(quote);

        // 7. Map to exact contract response using QuoteMapper
        return quoteMapper.toDto(savedQuote);
    }

    /**
     * Retrieve quote by ID.
     */
    @Transactional(readOnly = true)
    public Quote getQuoteById(UUID quoteId) {
        return quoteRepository.findById(quoteId)
                .orElseThrow(() -> new ResourceNotFoundException("Quote not found with ID: " + quoteId));
    }

    /**
     * Retrieves paginated quotes optionally filtered by lifecycle status.
     */
    @Transactional(readOnly = true)
    public Page<QuoteResponseDto> getAllQuotes(QuoteStatus status, Pageable pageable) {
        Page<Quote> quotes = (status != null)
                ? quoteRepository.findByStatus(status, pageable)
                : quoteRepository.findAll(pageable);
        return quotes.map(quoteMapper::toDto);
    }

    /**
     * Retrieves quote DTO by ID.
     */
    @Transactional(readOnly = true)
    public QuoteResponseDto getQuoteDtoById(UUID quoteId) {
        Quote quote = getQuoteById(quoteId);
        return quoteMapper.toDto(quote);
    }
}
