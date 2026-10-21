package com.extin.backend.models.enums;

/**
 * Commercial and fiscal lifecycle status for invoices:
 * - ISSUED: Officially generated and delivered to the customer.
 * - PAID: Fully settled and paid.
 * - CANCELLED: Voided according to fiscal regulations.
 */
public enum InvoiceStatus {
    ISSUED,
    PAID,
    CANCELLED
}
