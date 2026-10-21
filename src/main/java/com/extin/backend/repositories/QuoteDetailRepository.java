package com.extin.backend.repositories;

import com.extin.backend.models.entities.QuoteDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA Repository for QuoteDetail entities.
 */
@Repository
public interface QuoteDetailRepository extends JpaRepository<QuoteDetail, UUID> {

    /**
     * Find all line items belonging to a specific quote.
     */
    List<QuoteDetail> findByQuoteId(UUID quoteId);

    /**
     * Find all quote lines referencing a specific catalog item.
     */
    List<QuoteDetail> findByItemId(UUID itemId);
}

