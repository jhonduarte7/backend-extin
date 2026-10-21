package com.extin.backend.mcp.client.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Infrastructure configuration for Spring AI MCP client components.
 * Located in com.extin.backend.mcp.client.config as per Clean Architecture / SDD guidelines.
 *
 * Responsibilities:
 * - Declares technical infrastructure beans such as ChatMemory.
 * - Adheres to Separation of AI Concerns: System prompt and business orchestration
 *   reside exclusively in ChatService.
 */
@Configuration
public class McpClientConfig {

    /**
     * In-memory ChatMemory bean providing conversational history buffer.
     * Backed by InMemoryChatMemoryRepository with a rolling window of recent messages.
     *
     * @return ChatMemory bean instance.
     */
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
    }
}
