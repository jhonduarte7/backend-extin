package com.extin.backend.mappers;

import com.extin.backend.models.dtos.QuoteResponseDto;
import com.extin.backend.models.entities.Quote;
import org.springframework.stereotype.Component;

/**
 * Mapper responsible for transforming between Quote entities and QuoteResponseDto records.
 */
@Component
public class QuoteMapper {

    /**
     * Converts a Quote entity to QuoteResponseDto record.
     *
     * @param quote Quote entity.
     * @return QuoteResponseDto record or null if quote is null.
     */
    public QuoteResponseDto toDto(Quote quote) {
        if (quote == null) {
            return null;
        }

        return new QuoteResponseDto(
                quote.getId() != null ? quote.getId().toString() : null,
                quote.getStatus() != null ? quote.getStatus().name() : null,
                quote.getTargetCurrency(),
                quote.getExchangeRate(),
                quote.getTotalAmount(),
                quote.getConvertedTotalAmount(),
                "Quote generated successfully."
        );
    }
}
