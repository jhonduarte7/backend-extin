package com.extin.backend.controllers;

import com.extin.backend.models.dtos.CustomerResponseDto;
import com.extin.backend.services.CustomerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin REST Controller for Customer Management.
 * Endpoint: GET /api/admin/customers
 */
@RestController
@RequestMapping("/api/admin/customers")
public class AdminCustomerController {

    private final CustomerService customerService;

    public AdminCustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    /**
     * Search customers with pagination.
     * Default page size is 10 records per page.
     */
    @GetMapping
    public ResponseEntity<Page<CustomerResponseDto>> searchCustomers(
            @RequestParam(name = "query", required = false) String query,
            @PageableDefault(size = 10, sort = "businessName", direction = Sort.Direction.ASC) Pageable pageable) {

        Page<CustomerResponseDto> customers = customerService.searchCustomers(query, pageable);
        return ResponseEntity.ok(customers);
    }
}
