package com.extin.backend.models.dtos;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * Natural language chat query sent by the admin client to the MCP AI assistant.
 * Supports conversational continuity via optional chatId.
 */
public record ChatRequestDto(
        @JsonProperty("prompt")
        @JsonAlias({"message", "query", "text"})
        @NotBlank(message = "Prompt cannot be blank")
        String prompt,

        @JsonProperty("chat_id")
        @JsonAlias({"chatId", "conversation_id", "conversationId"})
        String chatId
) {
    public ChatRequestDto(String prompt) {
        this(prompt, null);
    }
}
