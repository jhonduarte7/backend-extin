package com.extin.backend.mappers;

import com.extin.backend.models.dtos.QuoteItemResponseDto;
import com.extin.backend.models.dtos.QuoteResponseDto;
import com.extin.backend.models.entities.Quote;
import org.springframework.stereotype.Component;

import java.util.List;

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

        List<QuoteItemResponseDto> itemDtos = null;
        if (quote.getDetails() != null) {
            itemDtos = quote.getDetails().stream().map(d -> new QuoteItemResponseDto(
                    d.getItem() != null ? d.getItem().getSkuCode() : null,
                    d.getItem() != null ? d.getItem().getName() : null,
                    d.getItem() != null && d.getItem().getType() != null ? d.getItem().getType().name() : "PRODUCT",
                    d.getQuantity(),
                    d.getUnitPrice(),
                    d.getSubtotal(),
                    d.getConvertedUnitPrice(),
                    d.getConvertedSubtotal()
            )).toList();
        }

        String customerName = quote.getCustomer() != null ? quote.getCustomer().getBusinessName() : null;
        String customerTaxId = quote.getCustomer() != null ? quote.getCustomer().getTaxId() : null;
        String customerPhone = quote.getCustomer() != null ? quote.getCustomer().getPhone() : null;
        String customerEmail = quote.getCustomer() != null ? quote.getCustomer().getEmail() : null;

        return new QuoteResponseDto(
                quote.getId() != null ? quote.getId().toString() : null,
                quote.getStatus() != null ? quote.getStatus().name() : null,
                quote.getTargetCurrency(),
                quote.getExchangeRate(),
                quote.getSubtotal(),
                quote.getAppliesIva(),
                quote.getTaxRate(),
                quote.getTaxAmount(),
                quote.getTotalAmount(),
                quote.getConvertedTotalAmount(),
                quote.getCustomerCity(),
                quote.getCustomerAddress(),
                quote.getDocumentNumber(),
                "Quote generated successfully.",
                customerName,
                customerTaxId,
                customerPhone,
                customerEmail,
                quote.getIssueDate(),
                itemDtos
        );
    }
}
