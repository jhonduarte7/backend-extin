package com.extin.backend.events;

import java.util.Collections;
import java.util.Map;

/**
 * Spring application event published when an entity needs asynchronous vectorization into VectorStore.
 * Conforms to Phase 3 (Real-time auto-vectorization) in docs/system-spec.md Section 7.
 */
public class EntityVectorizationEvent {

    private final String entityId;
    private final String entityType;
    private final String textContent;
    private final Map<String, Object> metadata;

    public EntityVectorizationEvent(String entityId, String entityType, String textContent, Map<String, Object> metadata) {
        this.entityId = entityId;
        this.entityType = entityType;
        this.textContent = textContent;
        this.metadata = metadata != null ? metadata : Collections.emptyMap();
    }

    public String getEntityId() {
        return entityId;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getTextContent() {
        return textContent;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return "EntityVectorizationEvent{" +
                "entityId='" + entityId + '\'' +
                ", entityType='" + entityType + '\'' +
                ", textContent='" + textContent + '\'' +
                '}';
    }
}
