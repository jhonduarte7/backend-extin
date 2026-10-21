package com.extin.backend.controllers;

import com.extin.backend.exceptions.GlobalExceptionHandler;
import com.extin.backend.models.dtos.DocumentUploadResponseDto;
import com.extin.backend.services.DocumentRagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests verifying {@link DocumentController} multipart file upload and RAG document ingestion.
 */
@ExtendWith(MockitoExtension.class)
class DocumentControllerTest {

    @Mock
    private DocumentRagService documentRagService;

    @InjectMocks
    private DocumentController documentController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(documentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/admin/documents/upload - Successfully uploads and vectorizes PDF with category")
    void testUploadPdfSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "manual_nfpa.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "%PDF-1.4 dummy content".getBytes()
        );

        DocumentUploadResponseDto expectedResponse = DocumentUploadResponseDto.success("manual_nfpa.pdf", "NFPA10", 5);
        when(documentRagService.uploadAndVectorizePdf(any(), eq("NFPA10"))).thenReturn(expectedResponse);

        mockMvc.perform(multipart("/api/admin/documents/upload")
                        .file(file)
                        .param("category", "NFPA10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.file_name").value("manual_nfpa.pdf"))
                .andExpect(jsonPath("$.category").value("NFPA10"))
                .andExpect(jsonPath("$.chunks_count").value(5))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").isNotEmpty());

        verify(documentRagService).uploadAndVectorizePdf(any(), eq("NFPA10"));
    }

    @Test
    @DisplayName("POST /api/admin/documents/upload - Succeeds without category and defaults to GENERAL")
    void testUploadPdfWithoutCategoryDefaultsToGeneral() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "manual_seguridad.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "%PDF-1.4 dummy content".getBytes()
        );

        DocumentUploadResponseDto expectedResponse = DocumentUploadResponseDto.success("manual_seguridad.pdf", "GENERAL", 3);
        when(documentRagService.uploadAndVectorizePdf(any(), eq("GENERAL"))).thenReturn(expectedResponse);

        mockMvc.perform(multipart("/api/admin/documents/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.file_name").value("manual_seguridad.pdf"))
                .andExpect(jsonPath("$.category").value("GENERAL"))
                .andExpect(jsonPath("$.chunks_count").value(3))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(documentRagService).uploadAndVectorizePdf(any(), eq("GENERAL"));
    }

    @Test
    @DisplayName("POST /api/admin/documents/upload - Handles validation errors (empty or invalid file)")
    void testUploadPdfValidationError() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                new byte[0]
        );

        when(documentRagService.uploadAndVectorizePdf(any(), eq("NFPA10")))
                .thenThrow(new IllegalArgumentException("El archivo PDF no puede estar vacío."));

        mockMvc.perform(multipart("/api/admin/documents/upload")
                        .file(file)
                        .param("category", "NFPA10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("El archivo PDF no puede estar vacío."));
    }

    @Test
    @DisplayName("POST /api/admin/documents/upload - Handles I/O internal errors")
    void testUploadPdfIoError() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "corrupt.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "corrupt bytes".getBytes()
        );

        when(documentRagService.uploadAndVectorizePdf(any(), eq("NFPA10")))
                .thenThrow(new IOException("Disk read error"));

        mockMvc.perform(multipart("/api/admin/documents/upload")
                        .file(file)
                        .param("category", "NFPA10"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Disk read error"));
    }
}
