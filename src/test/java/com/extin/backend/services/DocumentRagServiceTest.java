package com.extin.backend.services;

import com.extin.backend.models.dtos.DocumentUploadResponseDto;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Unit tests verifying PDF document parsing, token chunking, and pgvector storage in {@link DocumentRagService}.
 */
@ExtendWith(MockitoExtension.class)
class DocumentRagServiceTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private DocumentRagService documentRagService;

    @BeforeEach
    void setUp() {
        documentRagService = new DocumentRagService(vectorStore, jdbcTemplate);
    }

    private byte[] createSamplePdfBytes(String text) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(50, 700);
                stream.showText(text);
                stream.endText();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    @Test
    @DisplayName("uploadAndVectorizePdf - Successfully purges prior category chunks, processes, chunks and vectorizes valid PDF")
    void testUploadAndVectorizePdfSuccess() throws Exception {
        byte[] pdfBytes = createSamplePdfBytes("Manual Tecnico Extintor Portatil CO2 Seguridad Industrial.");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "manual-nfpa10.pdf",
                "application/pdf",
                pdfBytes
        );

        DocumentUploadResponseDto response = documentRagService.uploadAndVectorizePdf(file, "nfpa10");

        assertThat(response).isNotNull();
        assertThat(response.fileName()).isEqualTo("manual-nfpa10.pdf");
        assertThat(response.category()).isEqualTo("NFPA10");
        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.chunksCount()).isGreaterThanOrEqualTo(1);

        // Verify purge step was executed for the category and domain
        verify(jdbcTemplate).update(
                eq("DELETE FROM vector_store WHERE metadata->>'domain' = 'DOCUMENT' AND metadata->>'category' = ?"),
                eq("NFPA10")
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());

        List<Document> savedChunks = captor.getValue();
        assertThat(savedChunks).isNotEmpty();
        Document firstChunk = savedChunks.get(0);
        assertThat(firstChunk.getText()).contains("Extintor").contains("CO2");
        assertThat(firstChunk.getMetadata()).containsEntry("domain", "DOCUMENT");
        assertThat(firstChunk.getMetadata()).containsEntry("category", "NFPA10");
        assertThat(firstChunk.getMetadata()).containsEntry("file_name", "manual-nfpa10.pdf");
        assertThat(firstChunk.getMetadata()).containsEntry("document_type", "MANUAL_PDF");
    }

    @Test
    @DisplayName("uploadAndVectorizePdf - Rejects empty file")
    void testUploadAndVectorizePdfEmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.pdf",
                "application/pdf",
                new byte[0]
        );

        assertThatThrownBy(() -> documentRagService.uploadAndVectorizePdf(emptyFile, "NFPA"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vacío");
    }

    @Test
    @DisplayName("uploadAndVectorizePdf - Defaults to GENERAL when category is null or blank")
    void testUploadAndVectorizePdfBlankCategoryDefaultsToGeneral() throws Exception {
        byte[] pdfBytes = createSamplePdfBytes("Valid content for fire safety regulations.");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "valid.pdf",
                "application/pdf",
                pdfBytes
        );

        DocumentUploadResponseDto response = documentRagService.uploadAndVectorizePdf(file, "   ");
        assertThat(response.category()).isEqualTo("GENERAL");

        DocumentUploadResponseDto responseNull = documentRagService.uploadAndVectorizePdf(file, null);
        assertThat(responseNull.category()).isEqualTo("GENERAL");
    }

    @Test
    @DisplayName("uploadAndVectorizePdf - Rejects non-pdf file")
    void testUploadAndVectorizePdfNonPdf() {
        MockMultipartFile txtFile = new MockMultipartFile(
                "file",
                "documento.txt",
                "text/plain",
                "Texto plano".getBytes()
        );

        assertThatThrownBy(() -> documentRagService.uploadAndVectorizePdf(txtFile, "MANUAL"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("debe ser un documento PDF válido");
    }
}
