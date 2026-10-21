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

    @Column(name = "notes", length = 1000)
    private String notes;

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

    // Recalculate totals based on line items and exchange rate
    public void recalculateTotals() {
        BigDecimal sum = BigDecimal.ZERO;
        for (QuoteDetail detail : details) {
            if (detail.getSubtotal() != null) {
                sum = sum.add(detail.getSubtotal());
            }
        }
        this.totalAmount = sum;
        if (this.exchangeRate != null && this.exchangeRate.compareTo(BigDecimal.ZERO) > 0) {
            this.convertedTotalAmount = this.totalAmount.multiply(this.exchangeRate);
        }
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
        return totalAmount;
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
                ", baseCurrency='" + baseCurrency + '\'' +
                ", targetCurrency='" + targetCurrency + '\'' +
                ", exchangeRate=" + exchangeRate +
                ", totalAmount=" + totalAmount +
                ", convertedTotalAmount=" + convertedTotalAmount +
                '}';
    }
}

