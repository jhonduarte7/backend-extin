package com.extin.backend.controllers;

import com.extin.backend.models.dtos.QuoteResponseDto;
import com.extin.backend.models.enums.QuoteStatus;
import com.extin.backend.services.QuoteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin REST Controller for Quotes administration.
 * Endpoint: GET /api/admin/quotes (docs/api-contracts.md Section 2.3)
 */
@RestController
@RequestMapping("/api/admin/quotes")
public class AdminQuoteController {

    private final QuoteService quoteService;

    public AdminQuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    /**
     * Retrieves paginated quotes, optionally filtered by status (e.g. APPROVED, PENDING, INVOICED).
     */
    @GetMapping
    public ResponseEntity<Page<QuoteResponseDto>> getAllQuotes(
            @RequestParam(value = "status", required = false) QuoteStatus status,
            @PageableDefault(size = 5, sort = "issueDate", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(quoteService.getAllQuotes(status, pageable));
    }

    /**
     * Retrieves quote details by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<QuoteResponseDto> getQuoteById(@PathVariable("id") java.util.UUID id) {
        return ResponseEntity.ok(quoteService.getQuoteDtoById(id));
    }

    /**
     * Updates quote lifecycle status (e.g. APPROVED, PENDING, CANCELLED).
     */
    @org.springframework.web.bind.annotation.PatchMapping("/{id}/status")
    public ResponseEntity<QuoteResponseDto> updateQuoteStatus(
            @PathVariable("id") java.util.UUID id,
            @RequestParam("status") QuoteStatus status) {
        return ResponseEntity.ok(quoteService.updateQuoteStatus(id, status));
    }
}
