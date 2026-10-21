package com.extin.backend.repositories;

import com.extin.backend.models.entities.Quote;
import com.extin.backend.models.enums.QuoteStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA Repository for Quote entities.
 */
@Repository
public interface QuoteRepository extends JpaRepository<Quote, UUID> {

    /**
     * Find all quotes for a given customer ID ordered by issue date descending.
     */
    List<Quote> findByCustomerIdOrderByIssueDateDesc(UUID customerId);

    /**
     * Find quotes filtered by status.
     */
    List<Quote> findByStatus(QuoteStatus status);

    /**
     * Find paginated quotes filtered by status.
     */
    Page<Quote> findByStatus(QuoteStatus status, Pageable pageable);

    /**
     * Find quotes for a specific customer filtered by status.
     */
    List<Quote> findByCustomerIdAndStatus(UUID customerId, QuoteStatus status);
}

