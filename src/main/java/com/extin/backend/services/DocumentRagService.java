package com.extin.backend.services;

import com.extin.backend.models.dtos.DocumentUploadResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/**
 * Service responsible for processing, chunking, and vectorizing PDF documents
 * (manuals, safety regulations) into the pgvector VectorStore with category-based
 * purging and domain isolation.
 * Conforms to docs/system-spec.md Section 7.2 (Module 1: Document RAG).
 */
@Service
public class DocumentRagService {

    private static final Logger log = LoggerFactory.getLogger(DocumentRagService.class);

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    public DocumentRagService(VectorStore vectorStore, JdbcTemplate jdbcTemplate) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Uploads, extracts text, chunks, and vectorizes a PDF file into the VectorStore,
     * purging prior vectors for the given category to support clean replacement.
     *
     * @param file The uploaded MultipartFile representing a PDF document.
     * @param category Logical category or identifier for the manual (defaults to GENERAL if omitted).
     * @return DocumentUploadResponseDto containing metadata and number of chunks indexed.
     * @throws IOException If file operations fail.
     */
    public DocumentUploadResponseDto uploadAndVectorizePdf(MultipartFile file, String category) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo PDF no puede estar vacío.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("El archivo debe ser un documento PDF válido (.pdf).");
        }

        String normalizedCategory = (category != null && !category.trim().isBlank())
                ? category.trim().toUpperCase()
                : "GENERAL";

        log.info("DocumentRagService: Processing uploaded PDF file='{}', size={} bytes, category='{}'",
                originalFilename, file.getSize(), normalizedCategory);

        // PASO 1 (Purgado): Eliminar vectores antiguos de esta categoría para el dominio DOCUMENT
        String deleteSql = "DELETE FROM vector_store WHERE metadata->>'domain' = 'DOCUMENT' AND metadata->>'category' = ?";
        int deletedCount = jdbcTemplate.update(deleteSql, normalizedCategory);
        log.info("DocumentRagService: Purged {} existing vector chunk(s) for domain='DOCUMENT' and category='{}'",
                deletedCount, normalizedCategory);

        // PASO 2 (Extracción en memoria): Leer el PDF usando ByteArrayResource (evita problemas de bloqueo en disco)
        byte[] pdfBytes = file.getBytes();
        ByteArrayResource pdfResource = new ByteArrayResource(pdfBytes) {
            @Override
            public String getFilename() {
                return originalFilename;
            }
        };

        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(pdfResource);
        List<Document> rawDocuments = pdfReader.get();

        if (rawDocuments == null || rawDocuments.isEmpty()) {
            throw new IllegalArgumentException("No se pudo extraer texto del archivo PDF: " + originalFilename);
        }

        // Chunks de 400 tokens para garantizar compatibilidad con modelos de embedding como all-minilm (contexto 512)
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(400)
                .withMinChunkSizeChars(100)
                .build();
        List<Document> rawChunks = splitter.split(rawDocuments);

        List<Document> chunks = rawChunks.stream()
                .filter(chunk -> chunk.getText() != null && !chunk.getText().trim().isBlank())
                .toList();

        if (chunks.isEmpty()) {
            throw new IllegalArgumentException("No se encontró texto legible en el archivo PDF: " + originalFilename +
                    ". Verifique que el documento contenga texto seleccionable y no sea únicamente una imagen escaneada.");
        }

        // PASO 3 (Inyección de Metadatos): inyectar domain=DOCUMENT y category
        Instant now = Instant.now();
        for (int i = 0; i < chunks.size(); i++) {
            Document chunk = chunks.get(i);
            chunk.getMetadata().put("domain", "DOCUMENT");
            chunk.getMetadata().put("category", normalizedCategory);
            chunk.getMetadata().put("file_name", originalFilename);
            chunk.getMetadata().put("document_type", "MANUAL_PDF");
            chunk.getMetadata().put("chunk_index", i);
            chunk.getMetadata().put("total_chunks", chunks.size());
            chunk.getMetadata().put("upload_timestamp", now.toString());
        }

        // PASO 4 (Guardado): Guardar los nuevos chunks en VectorStore
        try {
            vectorStore.add(chunks);
        } catch (Exception ex) {
            log.error("DocumentRagService: Error guardando vectores en pgvector para '{}': ", originalFilename, ex);
            throw new IllegalStateException("Error al vectorizar en pgvector: " + ex.getMessage(), ex);
        }

        log.info("DocumentRagService: Successfully vectorized {} chunks into pgvector for '{}' [category='{}']",
                chunks.size(), originalFilename, normalizedCategory);

        return DocumentUploadResponseDto.success(originalFilename, normalizedCategory, chunks.size());
    }
}
