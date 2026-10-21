package com.extin.backend.repositories;

import com.extin.backend.models.entities.Invoice;
import com.extin.backend.models.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for Invoice entities.
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    /**
     * Find invoice by unique Venezuelan fiscal control number (if assigned).
     */
    Optional<Invoice> findByFiscalControlNumber(String fiscalControlNumber);

    /**
     * Check if an invoice exists with the given fiscal control number.
     */
    boolean existsByFiscalControlNumber(String fiscalControlNumber);

    /**
     * Find invoice generated from a specific quote ID.
     */
    Optional<Invoice> findByQuoteId(UUID quoteId);

    /**
     * Alias query for backwards compatibility with origin quote ID lookups.
     */
    @Query("SELECT i FROM Invoice i WHERE i.quote.id = :originQuoteId")
    Optional<Invoice> findByOriginQuoteId(@Param("originQuoteId") UUID originQuoteId);

    /**
     * Check if an invoice exists for a given quote ID.
     */
    boolean existsByQuoteId(UUID quoteId);

    /**
     * Find all invoices for a given customer ID ordered by issue date descending.
     */
    List<Invoice> findByCustomerIdOrderByIssueDateDesc(UUID customerId);

    /**
     * Find invoices filtered by lifecycle status.
     */
    List<Invoice> findByStatus(InvoiceStatus status);
}

