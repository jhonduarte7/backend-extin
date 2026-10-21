package com.extin.backend.controllers;

import com.extin.backend.models.dtos.CatalogItemDto;
import com.extin.backend.models.enums.ItemType;
import com.extin.backend.services.CatalogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public REST Controller for the Catalog.
 * Endpoint: GET /api/public/catalog
 */
@RestController
@RequestMapping("/api/public/catalog")
public class PublicCatalogController {

    private final CatalogService catalogService;

    public PublicCatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * Returns paginated list of products and services.
     * Default page size is 10 records per page. Prices are in USD base currency.
     */
    @GetMapping
    public ResponseEntity<Page<CatalogItemDto>> getCatalog(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "type", required = false) ItemType type,
            @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {

        Page<CatalogItemDto> catalogPage = catalogService.getCatalog(search, type, pageable);
        return ResponseEntity.ok(catalogPage);
    }
}
