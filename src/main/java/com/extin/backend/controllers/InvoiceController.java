package com.extin.backend.controllers;

import com.extin.backend.models.dtos.InvoiceResponseDto;
import com.extin.backend.models.dtos.PayAndIssueInvoiceRequestDto;
import com.extin.backend.services.InvoiceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST Controller for Invoicing.
 * Exposes:
 * - POST /api/admin/invoices/pay-and-issue/{quote_id} (docs/api-contracts.md Section 2.1)
 * - GET  /api/admin/invoices (docs/api-contracts.md Section 2.2)
 * - GET  /api/admin/invoices/{id} (docs/api-contracts.md Section 2.2)
 * - POST /api/admin/invoices/from-quote/{quote_id} (Legacy fiscal approval endpoint)
 */
@RestController
@RequestMapping("/api/admin/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    /**
     * Issues an immutable paid invoice from a quote upon payment notification.
     */
    @PostMapping("/pay-and-issue/{quote_id}")
    public ResponseEntity<InvoiceResponseDto> payAndIssueInvoice(
            @PathVariable("quote_id") UUID quoteId,
            @Valid @RequestBody PayAndIssueInvoiceRequestDto requestDto) {

        InvoiceResponseDto response = invoiceService.payAndIssueInvoice(quoteId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves paginated list of all invoices in the system.
     */
    @GetMapping
    public ResponseEntity<Page<InvoiceResponseDto>> getAllInvoices(
            @PageableDefault(size = 5, sort = "rateDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(invoiceService.getAllInvoices(pageable));
    }

    /**
     * Retrieves details of a specific invoice by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponseDto> getInvoiceById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(invoiceService.getInvoiceDtoById(id));
    }

    /**
     * Legacy endpoint to generate invoice from quote.
     */
    @PostMapping("/from-quote/{quote_id}")
    public ResponseEntity<InvoiceResponseDto> generateInvoiceFromQuote(
            @PathVariable("quote_id") UUID quoteId) {

        InvoiceResponseDto response = invoiceService.generateInvoiceFromQuote(quoteId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
