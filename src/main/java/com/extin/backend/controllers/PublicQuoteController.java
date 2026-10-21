package com.extin.backend.controllers;

import com.extin.backend.models.dtos.QuoteRequestDto;
import com.extin.backend.models.dtos.QuoteResponseDto;
import com.extin.backend.services.QuoteService;
import jakarta.validation.Valid;
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
 * Public REST Controller for Quote creation and lookup.
 * Endpoints:
 * - POST /api/public/quotes
 * - GET  /api/public/quotes/{id}
 */
@RestController
@RequestMapping("/api/public/quotes")
public class PublicQuoteController {

    private final QuoteService quoteService;

    public PublicQuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    /**
     * Receives a shopping cart from the public web and generates a quote using the
     * exchange rate for the requested currency.
     */
    @PostMapping
    public ResponseEntity<QuoteResponseDto> createQuote(@Valid @RequestBody QuoteRequestDto requestDto) {
        QuoteResponseDto response = quoteService.createQuote(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves details of a specific quote by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<QuoteResponseDto> getQuoteById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(quoteService.getQuoteDtoById(id));
    }
}
