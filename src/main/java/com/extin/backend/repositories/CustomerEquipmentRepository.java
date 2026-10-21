package com.extin.backend.repositories;

import com.extin.backend.models.entities.CustomerEquipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA Repository for CustomerEquipment entities.
 */
@Repository
public interface CustomerEquipmentRepository extends JpaRepository<CustomerEquipment, UUID> {

    /**
     * Find all equipment registered to a specific customer.
     */
    List<CustomerEquipment> findByCustomerId(UUID customerId);

    /**
     * Find equipment for a customer that expires before the given date.
     */
    List<CustomerEquipment> findByCustomerIdAndExpirationDateBefore(UUID customerId, LocalDate date);

    /**
     * Find all equipment across all customers expiring on or before a given date (for maintenance alerts).
     */
    List<CustomerEquipment> findByExpirationDateBefore(LocalDate date);
}

