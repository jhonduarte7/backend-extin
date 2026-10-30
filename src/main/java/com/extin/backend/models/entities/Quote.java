package com.extin.backend.models.entities;

import com.extin.backend.models.enums.QuoteStatus;
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
 * Entity representing a commercial Quote requested by or prepared for a customer.
 * Fully supports multi-currency operations (e.g., base USD to target VES/BCV).
 */
@Entity
@Table(name = "quotes")
public class Quote extends Auditory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "issue_date", nullable = false)
    private LocalDateTime issueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private QuoteStatus status = QuoteStatus.PENDING;

    // Multi-currency attributes
    @Column(name = "base_currency", nullable = false, length = 3)
    private String baseCurrency = "USD";

    @Column(name = "target_currency", length = 3)
    private String targetCurrency;

    @Column(name = "exchange_rate", precision = 19, scale = 4)
    private BigDecimal exchangeRate;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "converted_total_amount", precision = 19, scale = 2)
    private BigDecimal convertedTotalAmount;

    @Column(name = "applies_iva")
    private Boolean appliesIva = Boolean.FALSE;

    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate = BigDecimal.ZERO;

    @Column(name = "tax_amount", precision = 19, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "subtotal", precision = 19, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "customer_city", length = 100)
    private String customerCity;

    @Column(name = "customer_address", length = 500)
    private String customerAddress;

    @Column(name = "document_number", length = 50)
    private String documentNumber;

    @OneToMany(mappedBy = "quote", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuoteDetail> details = new ArrayList<>();

    public Quote() {
    }

    public Quote(Customer customer, LocalDateTime issueDate, QuoteStatus status, String baseCurrency, String targetCurrency, BigDecimal exchangeRate) {
        this.customer = customer;
        this.issueDate = issueDate != null ? issueDate : LocalDateTime.now();
        this.status = status != null ? status : QuoteStatus.PENDING;
        this.baseCurrency = baseCurrency != null ? baseCurrency : "USD";
        this.targetCurrency = targetCurrency;
        this.exchangeRate = exchangeRate;
        this.totalAmount = BigDecimal.ZERO;
    }

    // Bidirectional helper methods
    public void addDetail(QuoteDetail detail) {
        details.add(detail);
        detail.setQuote(this);
    }

    public void removeDetail(QuoteDetail detail) {
        details.remove(detail);
        detail.setQuote(null);
    }

    // Recalculate totals based on line items, tax rate, and exchange rate
    public void recalculateTotals() {
        BigDecimal sum = BigDecimal.ZERO;
        for (QuoteDetail detail : details) {
            if (detail.getSubtotal() != null) {
                sum = sum.add(detail.getSubtotal());
            }
        }
        this.subtotal = sum.setScale(2, RoundingMode.HALF_UP);

        if (Boolean.TRUE.equals(this.appliesIva) && this.taxRate != null && this.taxRate.compareTo(BigDecimal.ZERO) > 0) {
            this.taxAmount = this.subtotal.multiply(this.taxRate)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            this.totalAmount = this.subtotal.add(this.taxAmount).setScale(2, RoundingMode.HALF_UP);
        } else {
            this.taxAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            this.totalAmount = this.subtotal;
        }

        if (this.exchangeRate != null && this.exchangeRate.compareTo(BigDecimal.ZERO) > 0) {
            this.convertedTotalAmount = this.totalAmount.multiply(this.exchangeRate).setScale(2, RoundingMode.HALF_UP);
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
        if (totalAmount != null && totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            return totalAmount;
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

    public QuoteStatus getStatus() {
        return status;
    }

    public void setStatus(QuoteStatus status) {
        this.status = status;
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

    public BigDecimal getTotalAmount() {
        if (totalAmount != null && totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            return totalAmount;
        }
        if (subtotal != null && subtotal.compareTo(BigDecimal.ZERO) > 0) {
            return subtotal;
        }
        return totalAmount != null ? totalAmount : BigDecimal.ZERO;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getConvertedTotalAmount() {
        return convertedTotalAmount;
    }

    public void setConvertedTotalAmount(BigDecimal convertedTotalAmount) {
        this.convertedTotalAmount = convertedTotalAmount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCustomerCity() {
        return customerCity;
    }

    public void setCustomerCity(String customerCity) {
        this.customerCity = customerCity;
    }

    public String getCustomerAddress() {
        return customerAddress;
    }

    public void setCustomerAddress(String customerAddress) {
        this.customerAddress = customerAddress;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public List<QuoteDetail> getDetails() {
        return details;
    }

    public void setDetails(List<QuoteDetail> details) {
        this.details = details;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Quote quote)) return false;
        return Objects.equals(id, quote.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Quote{" +
                "id=" + id +
                ", issueDate=" + issueDate +
                ", status=" + status +
                ", customerCity='" + customerCity + '\'' +
                ", customerAddress='" + customerAddress + '\'' +
                ", documentNumber='" + documentNumber + '\'' +
                ", baseCurrency='" + baseCurrency + '\'' +
                ", targetCurrency='" + targetCurrency + '\'' +
                ", exchangeRate=" + exchangeRate +
                ", subtotal=" + subtotal +
                ", appliesIva=" + appliesIva +
                ", taxRate=" + taxRate +
                ", taxAmount=" + taxAmount +
                ", totalAmount=" + totalAmount +
                ", convertedTotalAmount=" + convertedTotalAmount +
                '}';
    }
}

