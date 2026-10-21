package com.extin.backend.mappers;

import com.extin.backend.models.dtos.CustomerInfoDto;
import com.extin.backend.models.dtos.CustomerResponseDto;
import com.extin.backend.models.entities.Customer;
import org.springframework.stereotype.Component;

/**
 * Mapper responsible for transforming between Customer entities and Customer DTO records.
 */
@Component
public class CustomerMapper {

    /**
     * Converts a Customer entity to CustomerResponseDto record.
     *
     * @param customer Customer entity.
     * @return CustomerResponseDto record or null if entity is null.
     */
    public CustomerResponseDto toDto(Customer customer) {
        if (customer == null) {
            return null;
        }

        return new CustomerResponseDto(
                customer.getId(),
                customer.getTaxId(),
                customer.getBusinessName(),
                customer.getAddress(),
                customer.getPhone(),
                customer.getEmail()
        );
    }

    /**
     * Creates a new Customer entity from CustomerInfoDto.
     *
     * @param dto CustomerInfoDto record.
     * @return Customer entity or null if dto is null.
     */
    public Customer toEntity(CustomerInfoDto dto) {
        if (dto == null) {
            return null;
        }

        Customer customer = new Customer();
        customer.setTaxId(dto.taxId());
        customer.setBusinessName(dto.businessName());
        customer.setEmail(dto.email());
        return customer;
    }
}
