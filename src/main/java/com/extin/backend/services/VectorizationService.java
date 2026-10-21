package com.extin.backend.services;

import com.extin.backend.events.EntityVectorizationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service orchestrating vector embeddings and pgvector storage via Spring AI's VectorStore.
 * Conforms to Phase 3 (Artificial Intelligence, RAG, and MCP Server) in docs/system-spec.md Section 7.
 * Provides:
 * 1. Direct vectorization methods for text and metadata.
 * 2. Non-blocking asynchronous real-time auto-vectorization triggered by EntityVectorizationEvent.
 */
@Service
public class VectorizationService {

    private static final Logger log = LoggerFactory.getLogger(VectorizationService.class);

    private final VectorStore vectorStore;

    public VectorizationService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * Vectorizes and stores text content along with metadata in pgvector.
     *
     * @param text Content describing the product, catalog item, or invoice.
     * @param metadata Map of contextual metadata attributes.
     * @return The generated or assigned document ID in the vector store.
     */
    public String saveVector(String text, Map<String, Object> metadata) {
        if (text == null || text.isBlank()) {
            log.warn("Attempted to vectorize empty or null text. Skipping.");
            return null;
        }

        String docId = UUID.randomUUID().toString();
        Map<String, Object> safeMetadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        Document document = new Document(docId, text, safeMetadata);

        log.debug("Vectorizing and saving document to VectorStore: ID={}, TextLength={}", docId, text.length());
        vectorStore.add(List.of(document));
        log.info("Document successfully vectorized into pgvector: ID={}", docId);
        return docId;
    }

    /**
     * Overload: Vectorizes and stores text content without additional metadata.
     *
     * @param text Content to vectorize.
     * @return The generated document ID.
     */
    public String saveVector(String text) {
        return saveVector(text, Map.of());
    }

    /**
     * Real-time auto-vectorization event listener.
     * Listens asynchronously to EntityVectorizationEvent so the main HTTP API thread is never blocked.
     */
    @Async
    @EventListener
    public void handleEntityVectorization(EntityVectorizationEvent event) {
        if (event == null || event.getTextContent() == null || event.getTextContent().isBlank()) {
            return;
        }

        log.info(">>> [ASYNC AUTO-VECTORIZATION TRIGGERED] EntityType={}, EntityID={}",
                event.getEntityType(), event.getEntityId());

        Map<String, Object> meta = new HashMap<>(event.getMetadata());
        meta.put("entityId", event.getEntityId());
        meta.put("entityType", event.getEntityType());

        try {
            saveVector(event.getTextContent(), meta);
            log.info(">>> [ASYNC AUTO-VECTORIZATION COMPLETE] Successfully embedded {} [{}]",
                    event.getEntityType(), event.getEntityId());
        } catch (Exception e) {
            log.error("Failed to auto-vectorize entity {} [{}]: {}",
                    event.getEntityType(), event.getEntityId(), e.getMessage(), e);
        }
    }
}
