package com.extin.backend.mcp.server.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Domain-specific tool for catalog recommendations and semantic product search.
 * Conforms to docs/system-spec.md Section 4 (Tool 1: tool_semantic_catalog_search).
 *
 * Optimizations:
 * - Bounds output results to a maximum of 4 items.
 * - Caps per-item text payload to 350 characters to prevent Llama 3.1 context saturation.
 */
@Component
public class CatalogTools {

    private static final Logger log = LoggerFactory.getLogger(CatalogTools.class);
    public static final int MAX_CATALOG_RESULTS = 4;
    public static final int MAX_ITEM_LENGTH = 350;

    private final VectorStore vectorStore;

    public CatalogTools(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * Executes semantic similarity search against catalog embeddings in pgvector.
     *
     * @param query Natural language user inquiry.
     * @return Formatted summary of matching catalog items bounded for context efficiency.
     */
    @Tool(name = "semanticProductSearch",
          description = "Usa esta herramienta SIEMPRE para buscar productos, extintores o recomendaciones en el catálogo usando el lenguaje natural del usuario")
    public String semanticProductSearch(
            @ToolParam(description = "Consulta en lenguaje natural sobre extintores, productos o servicios de seguridad")
            String query) {
        log.info("Executing CatalogTools.semanticProductSearch with query='{}'", query);

        if (query == null || query.isBlank()) {
            return "La consulta no puede estar vacía.";
        }

        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query.trim())
                    .topK(MAX_CATALOG_RESULTS)
                    .build();

            List<Document> documents = vectorStore.similaritySearch(searchRequest);

            if (documents == null || documents.isEmpty()) {
                return "No se encontraron productos o servicios en el catálogo que coincidan con la búsqueda: " + query;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Resultados del catálogo:\n");
            for (Document doc : documents) {
                String text = (doc.getText() != null) ? doc.getText().trim() : "";
                if (text.length() > MAX_ITEM_LENGTH) {
                    text = text.substring(0, MAX_ITEM_LENGTH) + "...";
                }
                sb.append("- ").append(text).append("\n");
            }

            return sb.toString();
        } catch (Exception e) {
            log.error("Error performing semantic product search for query='{}': {}", query, e.getMessage(), e);
            return "Error al consultar el catálogo vectorial: " + e.getMessage();
        }
    }
}
