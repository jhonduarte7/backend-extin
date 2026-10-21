package com.extin.backend.services;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Service managing daily multi-currency exchange rates relative to USD base currency.
 * Supports USD, Venezuelan Bolívares (VES / BCV rate), and Brazilian Reais (BRL).
 */
@Service
public class CurrencyExchangeService {

    public static final String CURRENCY_USD = "USD";
    public static final String CURRENCY_VES = "VES";
    public static final String CURRENCY_BRL = "BRL";

    // Standard official exchange rates (can be updated dynamically or via external rate feed)
    private static final BigDecimal RATE_USD = BigDecimal.ONE;
    private static final BigDecimal RATE_VES = new BigDecimal("36.50");
    private static final BigDecimal RATE_BRL = new BigDecimal("5.50");

    /**
     * Resolves the exchange rate for the requested target currency against 1 USD base.
     */
    public BigDecimal getExchangeRate(String targetCurrency) {
        if (targetCurrency == null || targetCurrency.isBlank()) {
            return RATE_USD;
        }

        String normalized = targetCurrency.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case CURRENCY_VES -> RATE_VES;
            case CURRENCY_BRL -> RATE_BRL;
            case CURRENCY_USD -> RATE_USD;
            default -> RATE_USD;
        };
    }

    /**
     * Normalizes currency string, defaulting to USD if unspecified.
     */
    public String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return CURRENCY_USD;
        }
        String normalized = currency.trim().toUpperCase(Locale.ROOT);
        if (CURRENCY_VES.equals(normalized) || CURRENCY_BRL.equals(normalized) || CURRENCY_USD.equals(normalized)) {
            return normalized;
        }
        return CURRENCY_USD;
    }
}
