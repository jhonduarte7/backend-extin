package com.extin.backend.services;

import com.extin.backend.exceptions.ResourceNotFoundException;
import com.extin.backend.mappers.CatalogItemMapper;
import com.extin.backend.models.dtos.CatalogItemDto;
import com.extin.backend.models.entities.CatalogItem;
import com.extin.backend.models.enums.ItemType;
import com.extin.backend.repositories.CatalogItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing products and services in the catalog.
 * Uses CatalogItemMapper for Entity to DTO transformations.
 */
@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final CatalogItemRepository catalogItemRepository;
    private final CatalogItemMapper catalogItemMapper;

    public CatalogService(CatalogItemRepository catalogItemRepository, CatalogItemMapper catalogItemMapper) {
        this.catalogItemRepository = catalogItemRepository;
        this.catalogItemMapper = catalogItemMapper;
    }

    /**
     * Retrieve paginated catalog items with optional filtering by name and item type.
     */
    public Page<CatalogItemDto> getCatalog(String search, ItemType type, Pageable pageable) {
        Page<CatalogItem> page;

        boolean hasSearch = (search != null && !search.isBlank());
        boolean hasType = (type != null);

        if (hasSearch && hasType) {
            page = catalogItemRepository.findByNameContainingIgnoreCaseAndTypeAndActiveTrue(search.trim(), type, pageable);
        } else if (hasSearch) {
            page = catalogItemRepository.findByNameContainingIgnoreCaseAndActiveTrue(search.trim(), pageable);
        } else if (hasType) {
            page = catalogItemRepository.findByTypeAndActiveTrue(type, pageable);
        } else {
            page = catalogItemRepository.findByActiveTrue(pageable);
        }

        return page.map(catalogItemMapper::toDto);
    }

    /**
     * Find active catalog item entity by SKU.
     */
    public CatalogItem getEntityBySku(String sku) {
        return catalogItemRepository.findBySkuCode(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Catalog item not found with SKU: " + sku));
    }

    /**
     * Entity to DTO mapping delegating to CatalogItemMapper.
     */
    public CatalogItemDto toDto(CatalogItem item) {
        return catalogItemMapper.toDto(item);
    }
}
