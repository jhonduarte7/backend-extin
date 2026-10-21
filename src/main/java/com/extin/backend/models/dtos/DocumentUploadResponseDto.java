package com.extin.backend.models.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO returned after successfully uploading and vectorizing a PDF document.
 */
public record DocumentUploadResponseDto(
        @JsonProperty("file_name")
        String fileName,

        @JsonProperty("category")
        String category,

        @JsonProperty("chunks_count")
        int chunksCount,

        @JsonProperty("status")
        String status,

        @JsonProperty("message")
        String message
) {
    public static DocumentUploadResponseDto success(String fileName, String category, int chunksCount) {
        return new DocumentUploadResponseDto(
                fileName,
                category,
                chunksCount,
                "SUCCESS",
                String.format("Documento '%s' [Categoría: %s] procesado y vectorizado exitosamente en %d fragmentos.", fileName, category, chunksCount)
        );
    }
}
