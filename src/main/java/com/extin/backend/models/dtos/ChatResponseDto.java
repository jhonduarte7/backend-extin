package com.extin.backend.models.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Natural language response returned by the MCP AI assistant.
 */
public record ChatResponseDto(
        @JsonProperty("reply")
        String reply,

        @JsonProperty("tools_used")
        List<String> toolsUsed,

        @JsonProperty("sources")
        List<String> sources,

        @JsonProperty("chat_id")
        String chatId
) {
    public ChatResponseDto(String reply, List<String> toolsUsed, List<String> sources) {
        this(reply, toolsUsed, sources, "default-chat-session");
    }
}
