package com.extin.backend.repositories;

import com.extin.backend.models.entities.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for Customer entities with pagination support.
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    /**
     * Find customer by tax identifier (RIF or CÃ©dula).
     */
    Optional<Customer> findByTaxId(String taxId);

    /**
     * Check if a customer already exists with the given tax ID.
     */
    boolean existsByTaxId(String taxId);

    /**
     * Search customers by business name (case-insensitive fuzzy search).
     */
    List<Customer> findByBusinessNameContainingIgnoreCase(String businessName);

    /**
     * Search customers by business name or tax ID with pagination.
     */
    Page<Customer> findByBusinessNameContainingIgnoreCaseOrTaxIdContainingIgnoreCase(String businessName, String taxId, Pageable pageable);
}

