package com.extin.backend.repositories;

import com.extin.backend.models.entities.CatalogItem;
import com.extin.backend.models.enums.ItemType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for CatalogItem entities with pagination support.
 */
@Repository
public interface CatalogItemRepository extends JpaRepository<CatalogItem, UUID> {

    /**
     * Find item by unique SKU code.
     */
    Optional<CatalogItem> findBySkuCode(String skuCode);

    /**
     * Check if an item exists by SKU code.
     */
    boolean existsBySkuCode(String skuCode);

    /**
     * Find all active items by their type (PRODUCT or SERVICE).
     */
    List<CatalogItem> findByTypeAndActiveTrue(ItemType type);

    /**
     * Find all active items for public catalog listing.
     */
    List<CatalogItem> findByActiveTrue();

    /**
     * Search active items by name containing search string (case-insensitive).
     */
    List<CatalogItem> findByNameContainingIgnoreCaseAndActiveTrue(String name);

    /**
     * Find active catalog items with pagination.
     */
    Page<CatalogItem> findByActiveTrue(Pageable pageable);

    /**
     * Search active items by name with pagination.
     */
    Page<CatalogItem> findByNameContainingIgnoreCaseAndActiveTrue(String name, Pageable pageable);

    /**
     * Find active items by type with pagination.
     */
    Page<CatalogItem> findByTypeAndActiveTrue(ItemType type, Pageable pageable);

    /**
     * Search active items by name and type with pagination.
     */
    Page<CatalogItem> findByNameContainingIgnoreCaseAndTypeAndActiveTrue(String name, ItemType type, Pageable pageable);
}

