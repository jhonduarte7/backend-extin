package com.extin.backend.models.entities;

import com.extin.backend.models.enums.ItemType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing products and services offered in the fire safety catalog:
 * - PRODUCT: New equipment (extinguishers, fire hose cabinets, smoke detectors).
 * - SERVICE: Maintenance, inspections, hydrostatic testing, refills.
 */
@Entity
@Table(name = "catalog_items")
@EntityListeners({AuditingEntityListener.class, CatalogItemEntityListener.class})
public class CatalogItem extends Auditory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "sku_code", nullable = false, unique = true, length = 50)
    private String skuCode;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ItemType type;

    @Column(name = "base_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "USD";

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    public CatalogItem() {
    }

    public CatalogItem(String skuCode, String name, ItemType type, BigDecimal basePrice) {
        this(skuCode, name, type, basePrice, "USD", null);
    }

    public CatalogItem(String skuCode, String name, ItemType type, BigDecimal basePrice, String currency, String imageUrl) {
        this.skuCode = skuCode;
        this.name = name;
        this.type = type;
        this.basePrice = basePrice;
        this.currency = (currency != null) ? currency : "USD";
        this.imageUrl = imageUrl;
        this.active = true;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public void setSkuCode(String skuCode) {
        this.skuCode = skuCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ItemType getType() {
        return type;
    }

    public void setType(ItemType type) {
        this.type = type;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CatalogItem item)) return false;
        return Objects.equals(id, item.id) || (skuCode != null && Objects.equals(skuCode, item.skuCode));
    }

    @Override
    public int hashCode() {
        return Objects.hash(skuCode);
    }

    @Override
    public String toString() {
        return "CatalogItem{" +
                "id=" + id +
                ", skuCode='" + skuCode + '\'' +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", basePrice=" + basePrice +
                ", currency='" + currency + '\'' +
                '}';
    }
}

