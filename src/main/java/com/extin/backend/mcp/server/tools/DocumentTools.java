package com.extin.backend.mcp.server.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Domain-specific MCP Tool for semantic document search (Document RAG) across
 * uploaded manuals, standards, and regulations.
 * Strictly filters by domain = 'DOCUMENT' to avoid collision with catalog product embeddings.
 * Conforms to docs/system-spec.md Section 4 (Tool 3: tool_rag_manuals_search).
 *
 * Optimizations:
 * - Bounds retrieved chunks to a maximum of 4.
 * - Caps per-chunk text payload to 700 characters to prevent Llama 3.1 context saturation.
 */
@Component
public class DocumentTools {

    private static final Logger log = LoggerFactory.getLogger(DocumentTools.class);
    public static final int MAX_DOCUMENT_CHUNKS = 4;
    public static final int MAX_CHUNK_LENGTH = 700;

    private final VectorStore vectorStore;

    public DocumentTools(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * Executes similarity search across vectorized documents and manuals,
     * restricted exclusively to domain == 'DOCUMENT'.
     *
     * @param query Natural language question about fire codes, installation, or manual instructions.
     * @return Extracted and formatted document chunks bounded for context efficiency.
     */
    @Tool(name = "searchInManuals",
          description = "Usa esta herramienta SIEMPRE para buscar respuestas sobre normativas, manuales, reglas de instalación o seguridad en los documentos PDF subidos al sistema")
    public String searchInManuals(
            @ToolParam(description = "Consulta sobre normativas, manuales, reglas de instalación o seguridad contra incendios")
            String query) {

        log.info("Executing DocumentTools.searchInManuals with query='{}'", query);

        if (query == null || query.isBlank()) {
            return "La consulta sobre manuales o normativas no puede estar vacía.";
        }

        try {
            // Filtrado explícito por domain == 'DOCUMENT' para aislar los documentos del catálogo de productos
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query.trim())
                    .topK(MAX_DOCUMENT_CHUNKS)
                    .filterExpression(new FilterExpressionBuilder().eq("domain", "DOCUMENT").build())
                    .build();

            List<Document> documents = vectorStore.similaritySearch(searchRequest);

            if (documents == null || documents.isEmpty()) {
                return "No se encontraron fragmentos relevantes en los manuales o normativas para la consulta: " + query;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("=== Fragmentos Relevantes de Manuales y Normativas ===\n\n");
            for (int i = 0; i < documents.size(); i++) {
                Document doc = documents.get(i);
                Object fileName = doc.getMetadata() != null ? doc.getMetadata().get("file_name") : null;
                Object category = doc.getMetadata() != null ? doc.getMetadata().get("category") : null;
                Object pageNumber = doc.getMetadata() != null ? doc.getMetadata().get("page_number") : null;

                String sourceInfo = fileName != null ? fileName.toString() : "Manual PDF";
                if (category != null) {
                    sourceInfo += " (" + category + ")";
                }
                if (pageNumber != null) {
                    sourceInfo += ", Pág: " + pageNumber;
                }

                String text = (doc.getText() != null) ? doc.getText().trim() : "";
                if (text.length() > MAX_CHUNK_LENGTH) {
                    text = text.substring(0, MAX_CHUNK_LENGTH) + "... [fragmento acotado]";
                }

                sb.append(String.format("--- [Fragmento %d | Fuente: %s] ---\n", i + 1, sourceInfo));
                sb.append(text).append("\n\n");
            }

            return sb.toString().trim();
        } catch (Exception e) {
            log.error("Error executing searchInManuals for query='{}': {}", query, e.getMessage(), e);
            return "Error al buscar en los manuales de seguridad: " + e.getMessage();
        }
    }
}
