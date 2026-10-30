package com.extin.backend.models.entities;

import com.extin.backend.models.enums.InvoiceStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing an immutable fiscal Invoice.
 * Conforms to docs/system-spec.md (Section 2.3 - Commercial Workflow / Payment-Driven Invoicing).
 * Generated immutably upon payment notification with exchange rate capture, USD and local amounts.
 */
@Entity
@Table(name = "invoices")
public class Invoice extends Auditory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id")
    private Quote quote;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InvoiceStatus status = InvoiceStatus.PAID;

    // Multi-currency attributes
    @Column(name = "base_currency", length = 3)
    private String baseCurrency = "USD";

    @Column(name = "target_currency", length = 3)
    private String targetCurrency;

    @Column(name = "exchange_rate", precision = 19, scale = 4)
    private BigDecimal exchangeRate;

    @Column(name = "total_amount_usd", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmountUsd = BigDecimal.ZERO;

    @Column(name = "total_amount_local", precision = 19, scale = 2)
    private BigDecimal totalAmountLocal;

    @Column(name = "applies_iva")
    private Boolean appliesIva = Boolean.FALSE;

    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate = BigDecimal.ZERO;

    @Column(name = "tax_amount", precision = 19, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "subtotal", precision = 19, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "rate_date")
    private LocalDateTime rateDate;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "fiscal_control_number", length = 50)
    private String fiscalControlNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "issue_date")
    private LocalDateTime issueDate;

    @Column(name = "notes", length = 1000)
    private String notes;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceDetail> details = new ArrayList<>();

    public Invoice() {
    }

    public Invoice(Quote quote, String targetCurrency, BigDecimal exchangeRate,
                   BigDecimal totalAmountUsd, BigDecimal totalAmountLocal,
                   LocalDateTime rateDate, String paymentReference) {
        this.quote = quote;
        this.status = InvoiceStatus.PAID;
        this.baseCurrency = "USD";
        this.targetCurrency = targetCurrency;
        this.exchangeRate = exchangeRate;
        this.totalAmountUsd = totalAmountUsd != null ? totalAmountUsd : BigDecimal.ZERO;
        this.totalAmountLocal = totalAmountLocal;
        this.rateDate = rateDate != null ? rateDate : LocalDateTime.now();
        this.paymentReference = paymentReference;
        this.issueDate = this.rateDate;
        if (quote != null) {
            this.customer = quote.getCustomer();
        }
    }

    // Bidirectional helper methods
    public void addDetail(InvoiceDetail detail) {
        details.add(detail);
        detail.setInvoice(this);
    }

    public void removeDetail(InvoiceDetail detail) {
        details.remove(detail);
        detail.setInvoice(null);
    }

    // Recalculate totals based on line items, tax rate, and exchange rate
    public void recalculateTotals() {
        BigDecimal sum = BigDecimal.ZERO;
        for (InvoiceDetail detail : details) {
            if (detail.getSubtotal() != null) {
                sum = sum.add(detail.getSubtotal());
            }
        }
        this.subtotal = sum.setScale(2, RoundingMode.HALF_UP);

        if (Boolean.TRUE.equals(this.appliesIva) && this.taxRate != null && this.taxRate.compareTo(BigDecimal.ZERO) > 0) {
            this.taxAmount = this.subtotal.multiply(this.taxRate)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            this.totalAmountUsd = this.subtotal.add(this.taxAmount).setScale(2, RoundingMode.HALF_UP);
        } else {
            this.taxAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            this.totalAmountUsd = this.subtotal;
        }

        if (this.exchangeRate != null && this.exchangeRate.compareTo(BigDecimal.ZERO) > 0) {
            this.totalAmountLocal = this.totalAmountUsd.multiply(this.exchangeRate).setScale(2, RoundingMode.HALF_UP);
        }
    }

    public Boolean getAppliesIva() {
        return appliesIva;
    }

    public void setAppliesIva(Boolean appliesIva) {
        this.appliesIva = appliesIva;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getSubtotal() {
        if (subtotal != null && subtotal.compareTo(BigDecimal.ZERO) > 0) {
            return subtotal;
        }
        if (totalAmountUsd != null && totalAmountUsd.compareTo(BigDecimal.ZERO) > 0) {
            return totalAmountUsd;
        }
        return subtotal != null ? subtotal : BigDecimal.ZERO;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Quote getQuote() {
        return quote;
    }

    public void setQuote(Quote quote) {
        this.quote = quote;
        if (quote != null && this.customer == null) {
            this.customer = quote.getCustomer();
        }
    }

    // Alias for backward compatibility
    public Quote getOriginQuote() {
        return quote;
    }

    public void setOriginQuote(Quote quote) {
        setQuote(quote);
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(InvoiceStatus status) {
        this.status = status;
    }

    public void setStatus(String statusStr) {
        if (statusStr != null) {
            this.status = InvoiceStatus.valueOf(statusStr.trim().toUpperCase());
        }
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public String getTargetCurrency() {
        return targetCurrency;
    }

    public void setTargetCurrency(String targetCurrency) {
        this.targetCurrency = targetCurrency;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public BigDecimal getTotalAmountUsd() {
        return totalAmountUsd;
    }

    public void setTotalAmountUsd(BigDecimal totalAmountUsd) {
        this.totalAmountUsd = totalAmountUsd;
    }

    // Compatibility aliases
    public BigDecimal getTotalAmount() {
        return totalAmountUsd;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmountUsd = totalAmount;
    }

    public BigDecimal getTotalAmountLocal() {
        return totalAmountLocal;
    }

    public void setTotalAmountLocal(BigDecimal totalAmountLocal) {
        this.totalAmountLocal = totalAmountLocal;
    }

    public BigDecimal getConvertedTotalAmount() {
        return totalAmountLocal;
    }

    public void setConvertedTotalAmount(BigDecimal convertedTotalAmount) {
        this.totalAmountLocal = convertedTotalAmount;
    }

    public LocalDateTime getRateDate() {
        return rateDate;
    }

    public void setRateDate(LocalDateTime rateDate) {
        this.rateDate = rateDate;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public String getFiscalControlNumber() {
        return fiscalControlNumber;
    }

    public void setFiscalControlNumber(String fiscalControlNumber) {
        this.fiscalControlNumber = fiscalControlNumber;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public LocalDateTime getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDateTime issueDate) {
        this.issueDate = issueDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<InvoiceDetail> getDetails() {
        return details;
    }

    public void setDetails(List<InvoiceDetail> details) {
        this.details = details;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Invoice invoice)) return false;
        return Objects.equals(id, invoice.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Invoice{" +
                "id=" + id +
                ", quote=" + (quote != null ? quote.getId() : null) +
                ", status=" + status +
                ", targetCurrency='" + targetCurrency + '\'' +
                ", exchangeRate=" + exchangeRate +
                ", subtotal=" + subtotal +
                ", appliesIva=" + appliesIva +
                ", taxRate=" + taxRate +
                ", taxAmount=" + taxAmount +
                ", totalAmountUsd=" + totalAmountUsd +
                ", totalAmountLocal=" + totalAmountLocal +
                ", rateDate=" + rateDate +
                ", paymentReference='" + paymentReference + '\'' +
                '}';
    }
}

