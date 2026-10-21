package com.extin.backend.controllers;

import com.extin.backend.models.dtos.DocumentUploadResponseDto;
import com.extin.backend.services.DocumentRagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Administrative REST Controller for Document RAG management.
 * Endpoint: POST /api/admin/documents/upload
 *
 * Conforms to docs/system-spec.md Section 7.2 (Module 1: Document RAG).
 */
@RestController
@RequestMapping("/api/admin/documents")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentRagService documentRagService;

    public DocumentController(DocumentRagService documentRagService) {
        this.documentRagService = documentRagService;
    }

    /**
     * Uploads and vectorizes a PDF manual or safety regulation into the pgvector store,
     * replacing any existing vectors under the same category.
     *
     * @param file The PDF document uploaded as multipart form data.
     * @param category The logical category identifying the document (e.g. NFPA, COVENIN, TURISMO).
     * @return DocumentUploadResponseDto with upload summary.
     * @throws IOException If I/O processing fails.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DocumentUploadResponseDto> uploadPdfDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", required = false, defaultValue = "GENERAL") String category) throws IOException {

        log.info("DocumentController: Received request to upload and vectorize document: file={}, category={}",
                file != null ? file.getOriginalFilename() : "null", category);

        DocumentUploadResponseDto response = documentRagService.uploadAndVectorizePdf(file, category);
        return ResponseEntity.ok(response);
    }
}
