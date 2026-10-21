package com.extin.backend.repositories;

import com.extin.backend.models.entities.InvoiceDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA Repository for InvoiceDetail entities.
 */
@Repository
public interface InvoiceDetailRepository extends JpaRepository<InvoiceDetail, UUID> {

    /**
     * Find all line items for a specific invoice.
     */
    List<InvoiceDetail> findByInvoiceId(UUID invoiceId);

    /**
     * Find all invoice lines referencing a specific catalog item.
     */
    List<InvoiceDetail> findByItemId(UUID itemId);
}

