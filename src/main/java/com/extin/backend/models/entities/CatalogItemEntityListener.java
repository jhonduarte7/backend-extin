package com.extin.backend.models.entities;

import com.extin.backend.events.EntityVectorizationEvent;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * JPA EntityListener that triggers real-time auto-vectorization events
 * upon @PostPersist and @PostUpdate lifecycle phases of CatalogItem.
 * Conforms to docs/system-spec.md Section 7.2.
 */
@Component
public class CatalogItemEntityListener {

    private static final Logger log = LoggerFactory.getLogger(CatalogItemEntityListener.class);

    private static ApplicationEventPublisher eventPublisher;

    @Autowired
    public void setEventPublisher(ApplicationEventPublisher publisher) {
        CatalogItemEntityListener.eventPublisher = publisher;
    }

    @PostPersist
    public void onPostPersist(CatalogItem item) {
        log.debug("CatalogItem @PostPersist lifecycle triggered for SKU: {}", item.getSkuCode());
        publishVectorizationEvent(item, "PERSIST");
    }

    @PostUpdate
    public void onPostUpdate(CatalogItem item) {
        log.debug("CatalogItem @PostUpdate lifecycle triggered for SKU: {}", item.getSkuCode());
        publishVectorizationEvent(item, "UPDATE");
    }

    private void publishVectorizationEvent(CatalogItem item, String operation) {
        if (eventPublisher == null || item == null) {
            log.warn("Cannot publish EntityVectorizationEvent: publisher or item is null");
            return;
        }

        // Build rich descriptive text for semantic embeddings
        StringBuilder textBuilder = new StringBuilder();
        textBuilder.append("Product/Service: ").append(item.getName() != null ? item.getName() : "");
        textBuilder.append(". SKU: ").append(item.getSkuCode() != null ? item.getSkuCode() : "");
        textBuilder.append(". Type: ").append(item.getType() != null ? item.getType().name() : "");
        textBuilder.append(". Price: ").append(item.getBasePrice() != null ? item.getBasePrice() : "0.00").append(" ").append(item.getCurrency() != null ? item.getCurrency() : "USD");
        if (item.getDescription() != null && !item.getDescription().isBlank()) {
            textBuilder.append(". Description: ").append(item.getDescription());
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("sku", item.getSkuCode() != null ? item.getSkuCode() : "");
        metadata.put("name", item.getName() != null ? item.getName() : "");
        metadata.put("type", item.getType() != null ? item.getType().name() : "");
        metadata.put("currency", item.getCurrency() != null ? item.getCurrency() : "USD");
        if (item.getBasePrice() != null) {
            metadata.put("price", item.getBasePrice().doubleValue());
        }
        metadata.put("operation", operation);

        EntityVectorizationEvent event = new EntityVectorizationEvent(
                item.getId() != null ? item.getId().toString() : item.getSkuCode(),
                "CatalogItem",
                textBuilder.toString(),
                metadata
        );

        eventPublisher.publishEvent(event);
    }
}

