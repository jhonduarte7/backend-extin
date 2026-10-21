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
 */
@Component
public class CatalogTools {

    private static final Logger log = LoggerFactory.getLogger(CatalogTools.class);

    private final VectorStore vectorStore;

    public CatalogTools(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * Executes semantic similarity search against catalog embeddings in pgvector.
     *
     * @param query Natural language user inquiry.
     * @return Formatted summary of matching catalog items.
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
                    .query(query)
                    .topK(4)
                    .build();

            List<Document> documents = vectorStore.similaritySearch(searchRequest);

            if (documents == null || documents.isEmpty()) {
                return "No se encontraron productos o servicios en el catálogo que coincidan con la búsqueda: " + query;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Resultados del catálogo:\n");
            for (Document doc : documents) {
                sb.append("- ").append(doc.getText()).append("\n");
            }

            return sb.toString();
        } catch (Exception e) {
            log.error("Error performing semantic product search for query='{}': {}", query, e.getMessage(), e);
            return "Error al consultar el catálogo vectorial: " + e.getMessage();
        }
    }
}
