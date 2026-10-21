package com.extin.backend.services;

import com.extin.backend.mappers.CustomerMapper;
import com.extin.backend.models.dtos.CustomerInfoDto;
import com.extin.backend.models.dtos.CustomerResponseDto;
import com.extin.backend.models.entities.Customer;
import com.extin.backend.repositories.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing customer records and paginated search.
 * Uses CustomerMapper for Entity to DTO transformations.
 */
@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    public CustomerService(CustomerRepository customerRepository, CustomerMapper customerMapper) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
    }

    /**
     * Search customers with pagination. If query is provided, searches business name or tax ID.
     */
    @Transactional(readOnly = true)
    public Page<CustomerResponseDto> searchCustomers(String query, Pageable pageable) {
        Page<Customer> page;
        if (query != null && !query.isBlank()) {
            String trimmed = query.trim();
            page = customerRepository.findByBusinessNameContainingIgnoreCaseOrTaxIdContainingIgnoreCase(trimmed, trimmed, pageable);
        } else {
            page = customerRepository.findAll(pageable);
        }
        return page.map(customerMapper::toDto);
    }

    /**
     * Finds existing customer by Tax ID or creates a new record.
     */
    @Transactional
    public Customer findOrCreateCustomer(CustomerInfoDto dto) {
        return customerRepository.findByTaxId(dto.taxId().trim())
                .map(existing -> {
                    if (dto.businessName() != null && !dto.businessName().isBlank()) {
                        existing.setBusinessName(dto.businessName().trim());
                    }
                    if (dto.email() != null && !dto.email().isBlank()) {
                        existing.setEmail(dto.email().trim());
                    }
                    return customerRepository.save(existing);
                })
                .orElseGet(() -> {
                    Customer newCustomer = customerMapper.toEntity(dto);
                    return customerRepository.save(newCustomer);
                });
    }

    /**
     * Entity to DTO mapping delegating to CustomerMapper.
     */
    public CustomerResponseDto toDto(Customer customer) {
        return customerMapper.toDto(customer);
    }
}
