package com.extin.backend.models.enums;

/**
 * Commercial workflow lifecycle status for quotes:
 * - PENDING: Initial state, quote submitted or draft.
 * - APPROVED: Approved by the customer / manager, ready for invoicing.
 * - REJECTED: Declined by the customer.
 * - INVOICED: Facturado - Converted into an official cash invoice (factura de contado).
 */
public enum QuoteStatus {
    PENDING,
    APPROVED,
    REJECTED,
    INVOICED
}
