package com.extin.backend.services;

import com.extin.backend.events.EntityVectorizationEvent;
import com.extin.backend.models.entities.CatalogItem;
import com.extin.backend.models.entities.CatalogItemEntityListener;
import com.extin.backend.models.enums.ItemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests verifying vector storage and asynchronous auto-vectorization event handling in {@link VectorizationService}.
 */
@ExtendWith(MockitoExtension.class)
class VectorizationServiceTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private VectorizationService vectorizationService;

    @BeforeEach
    void setUp() {
        vectorizationService = new VectorizationService(vectorStore);
    }

    @Test
    @DisplayName("saveVector with metadata should create Document and call vectorStore.add")
    void testSaveVectorWithMetadata() {
        String text = "Extintor PQS 10 Lbs para fuegos ABC";
        Map<String, Object> metadata = Map.of("sku", "EXT-PQS-10", "price", 38.50);

        String docId = vectorizationService.saveVector(text, metadata);

        assertThat(docId).isNotNull();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore, times(1)).add(captor.capture());

        List<Document> savedDocs = captor.getValue();
        assertThat(savedDocs).hasSize(1);
        Document savedDoc = savedDocs.get(0);
        assertThat(savedDoc.getText()).isEqualTo(text);
        assertThat(savedDoc.getMetadata()).containsEntry("sku", "EXT-PQS-10");
        assertThat(savedDoc.getMetadata()).containsEntry("price", 38.50);
    }

    @Test
    @DisplayName("saveVector with blank or null text should skip vectorStore invocation")
    void testSaveVectorBlankOrNull() {
        String id1 = vectorizationService.saveVector(null, Map.of());
        String id2 = vectorizationService.saveVector("   ", Map.of());

        assertThat(id1).isNull();
        assertThat(id2).isNull();
        verify(vectorStore, never()).add(any());
    }

    @Test
    @DisplayName("handleEntityVectorization event listener should store document with entity metadata")
    void testHandleEntityVectorization() {
        EntityVectorizationEvent event = new EntityVectorizationEvent(
                "item-uuid-123",
                "CatalogItem",
                "Product: Extintor CO2 5 Lbs. SKU: EXT-CO2-5",
                Map.of("sku", "EXT-CO2-5", "type", "PRODUCT")
        );

        vectorizationService.handleEntityVectorization(event);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore, times(1)).add(captor.capture());

        List<Document> savedDocs = captor.getValue();
        assertThat(savedDocs).hasSize(1);
        Document doc = savedDocs.get(0);
        assertThat(doc.getText()).isEqualTo("Product: Extintor CO2 5 Lbs. SKU: EXT-CO2-5");
        assertThat(doc.getMetadata()).containsEntry("entityId", "item-uuid-123");
        assertThat(doc.getMetadata()).containsEntry("entityType", "CatalogItem");
        assertThat(doc.getMetadata()).containsEntry("sku", "EXT-CO2-5");
    }

    @Test
    @DisplayName("CatalogItemEntityListener should publish EntityVectorizationEvent on PostPersist and PostUpdate")
    void testCatalogItemEntityListener() {
        CatalogItemEntityListener listener = new CatalogItemEntityListener();
        listener.setEventPublisher(eventPublisher);

        CatalogItem item = new CatalogItem(
                "EXT-SOLK-10",
                "Extintor Solkaflam 10 Lbs",
                ItemType.PRODUCT,
                new BigDecimal("145.00"),
                "USD",
                "https://assets.extin.com/solk.png"
        );
        item.setId(UUID.randomUUID());
        item.setDescription("Agente limpio ecológico para data centers.");

        listener.onPostPersist(item);
        listener.onPostUpdate(item);

        ArgumentCaptor<EntityVectorizationEvent> captor = ArgumentCaptor.forClass(EntityVectorizationEvent.class);
        verify(eventPublisher, times(2)).publishEvent(captor.capture());

        List<EntityVectorizationEvent> events = captor.getAllValues();
        assertThat(events).hasSize(2);

        EntityVectorizationEvent firstEvent = events.get(0);
        assertThat(firstEvent.getEntityType()).isEqualTo("CatalogItem");
        assertThat(firstEvent.getTextContent()).contains("EXT-SOLK-10", "Extintor Solkaflam 10 Lbs", "145.00");
        assertThat(firstEvent.getMetadata()).containsEntry("sku", "EXT-SOLK-10");
        assertThat(firstEvent.getMetadata()).containsEntry("operation", "PERSIST");

        EntityVectorizationEvent secondEvent = events.get(1);
        assertThat(secondEvent.getMetadata()).containsEntry("operation", "UPDATE");
    }
}
