package com.extin.backend.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing client's physical equipment (e.g., fire extinguishers).
 * Tracks maintenance cycles, refill history, and expiration dates.
 */
@Entity
@Table(name = "customer_equipment")
public class CustomerEquipment extends Auditory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "equipment_type", nullable = false, length = 100)
    private String equipmentType;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Column(name = "location", length = 150)
    private String location;

    @Column(name = "last_refill_date")
    private LocalDate lastRefillDate;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "notes", length = 500)
    private String notes;

    public CustomerEquipment() {
    }

    public CustomerEquipment(Customer customer, String equipmentType, LocalDate lastRefillDate, LocalDate expirationDate) {
        this.customer = customer;
        this.equipmentType = equipmentType;
        this.lastRefillDate = lastRefillDate;
        this.expirationDate = expirationDate;
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

    public String getEquipmentType() {
        return equipmentType;
    }

    public void setEquipmentType(String equipmentType) {
        this.equipmentType = equipmentType;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDate getLastRefillDate() {
        return lastRefillDate;
    }

    public void setLastRefillDate(LocalDate lastRefillDate) {
        this.lastRefillDate = lastRefillDate;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomerEquipment that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "CustomerEquipment{" +
                "id=" + id +
                ", equipmentType='" + equipmentType + '\'' +
                ", serialNumber='" + serialNumber + '\'' +
                ", expirationDate=" + expirationDate +
                '}';
    }
}

