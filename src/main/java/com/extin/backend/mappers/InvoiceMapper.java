package com.extin.backend.mappers;

import com.extin.backend.models.dtos.InvoiceResponseDto;
import com.extin.backend.models.entities.Invoice;
import org.springframework.stereotype.Component;

/**
 * Mapper responsible for transforming between Invoice entities and InvoiceResponseDto records.
 */
@Component
public class InvoiceMapper {

    /**
     * Converts an Invoice entity to InvoiceResponseDto record (conforms to Section 2.1 Enfoque B).
     *
     * @param invoice Invoice entity.
     * @return InvoiceResponseDto record or null if entity is null.
     */
    public InvoiceResponseDto toDto(Invoice invoice) {
        if (invoice == null) {
            return null;
        }

        String quoteIdStr = invoice.getQuote() != null ? invoice.getQuote().getId().toString() :
                (invoice.getOriginQuote() != null ? invoice.getOriginQuote().getId().toString() : null);

        String customerName = invoice.getCustomer() != null ? invoice.getCustomer().getBusinessName() : null;
        String customerTaxId = invoice.getCustomer() != null ? invoice.getCustomer().getTaxId() : null;

        return new InvoiceResponseDto(
                invoice.getId() != null ? invoice.getId().toString() : null,
                quoteIdStr,
                invoice.getStatus() != null ? invoice.getStatus().name() : null,
                invoice.getTargetCurrency(),
                invoice.getExchangeRate(),
                invoice.getTotalAmountUsd(),
                invoice.getTotalAmountLocal(),
                invoice.getRateDate(),
                invoice.getPaymentReference(),
                invoice.getFiscalControlNumber(),
                quoteIdStr,
                customerName,
                customerTaxId,
                invoice.getIssueDate(),
                invoice.getBaseCurrency(),
                "Invoice generated successfully from quote."
        );
    }

    /**
     * Converts a paid invoice into the exact Section 2.1 response format.
     */
    public InvoiceResponseDto toPaidInvoiceDto(Invoice invoice) {
        if (invoice == null) {
            return null;
        }

        String quoteIdStr = invoice.getQuote() != null ? invoice.getQuote().getId().toString() :
                (invoice.getOriginQuote() != null ? invoice.getOriginQuote().getId().toString() : null);

        return InvoiceResponseDto.forPaidInvoice(
                invoice.getId() != null ? invoice.getId().toString() : null,
                quoteIdStr,
                invoice.getStatus() != null ? invoice.getStatus().name() : "PAID",
                invoice.getTargetCurrency(),
                invoice.getExchangeRate(),
                invoice.getTotalAmountUsd(),
                invoice.getTotalAmountLocal(),
                invoice.getRateDate(),
                invoice.getPaymentReference()
        );
    }
}
