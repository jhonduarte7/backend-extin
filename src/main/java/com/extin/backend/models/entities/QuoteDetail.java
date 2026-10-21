package com.extin.backend.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Line item detail for a quote.
 * Captures the agreed quantity, base unit price, subtotal, and converted multi-currency values.
 */
@Entity
@Table(name = "quote_details")
public class QuoteDetail extends Auditory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_id", nullable = false)
    private Quote quote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private CatalogItem item;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "subtotal", nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;

    // Multi-currency converted line-item values
    @Column(name = "converted_unit_price", precision = 19, scale = 2)
    private BigDecimal convertedUnitPrice;

    @Column(name = "converted_subtotal", precision = 19, scale = 2)
    private BigDecimal convertedSubtotal;

    public QuoteDetail() {
    }

    public QuoteDetail(Quote quote, CatalogItem item, Integer quantity, BigDecimal unitPrice) {
        this.quote = quote;
        this.item = item;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        calculateSubtotals();
    }

    @PrePersist
    @PreUpdate
    public void calculateSubtotals() {
        if (quantity != null && unitPrice != null) {
            this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
        if (quote != null && quote.getExchangeRate() != null && quote.getExchangeRate().compareTo(BigDecimal.ZERO) > 0) {
            if (this.unitPrice != null) {
                this.convertedUnitPrice = this.unitPrice.multiply(quote.getExchangeRate());
            }
            if (this.subtotal != null) {
                this.convertedSubtotal = this.subtotal.multiply(quote.getExchangeRate());
            }
        }
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
    }

    public CatalogItem getItem() {
        return item;
    }

    public void setItem(CatalogItem item) {
        this.item = item;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getConvertedUnitPrice() {
        return convertedUnitPrice;
    }

    public void setConvertedUnitPrice(BigDecimal convertedUnitPrice) {
        this.convertedUnitPrice = convertedUnitPrice;
    }

    public BigDecimal getConvertedSubtotal() {
        return convertedSubtotal;
    }

    public void setConvertedSubtotal(BigDecimal convertedSubtotal) {
        this.convertedSubtotal = convertedSubtotal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QuoteDetail that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "QuoteDetail{" +
                "id=" + id +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", subtotal=" + subtotal +
                ", convertedSubtotal=" + convertedSubtotal +
                '}';
    }
}

