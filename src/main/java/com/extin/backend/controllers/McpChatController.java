package com.extin.backend.controllers;

import com.extin.backend.models.dtos.ChatRequestDto;
import com.extin.backend.models.dtos.ChatResponseDto;
import com.extin.backend.services.ChatService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

/**
 * REST Controller for the MCP AI Assistant.
 * Endpoint: POST /api/admin/chat/mcp
 *
 * Responsibilities:
 * - Exposes HTTP endpoint for admin MCP chatbot.
 * - Handles JSON serialization/deserialization and content negotiation.
 * - Delegates all business logic, prompt orchestration, and tool execution to ChatService.
 */
@RestController
@RequestMapping("/api/admin/chat/mcp")
public class McpChatController {

    private static final Logger log = LoggerFactory.getLogger(McpChatController.class);

    private final ChatService chatService;

    public McpChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Processes admin chat queries with MCP tools and conversation memory enabled.
     *
     * @param requestDto JSON payload containing the user prompt and optional chatId.
     * @param headerChatId Optional HTTP header for conversation isolation (X-Chat-Id).
     * @param raw Optional boolean parameter to return raw Markdown text directly.
     * @param acceptHeader Client HTTP Accept header.
     * @return ChatResponseDto containing Markdown reply or raw Markdown body.
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE})
    public ResponseEntity<?> chat(
            @Valid @RequestBody ChatRequestDto requestDto,
            @RequestHeader(value = "X-Chat-Id", required = false) String headerChatId,
            @RequestParam(name = "raw", defaultValue = "false") boolean raw,
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String acceptHeader) {

        // Resolve chatId from header or DTO body, defaulting to "default-chat-session"
        String effectiveChatId = (headerChatId != null && !headerChatId.isBlank())
                ? headerChatId
                : (requestDto.chatId() != null && !requestDto.chatId().isBlank())
                    ? requestDto.chatId()
                    : "default-chat-session";

        log.info("McpChatController: Received chat request for chatId='{}' with prompt='{}'",
                effectiveChatId, requestDto.prompt());

        String markdownReply = chatService.processChat(effectiveChatId, requestDto.prompt());

        // Return raw Markdown text if requested explicitly or via Accept header
        if (raw || (acceptHeader != null && (acceptHeader.contains("text/markdown") || acceptHeader.contains(MediaType.TEXT_PLAIN_VALUE)))) {
            return ResponseEntity.ok()
                    .header("X-Chat-Id", effectiveChatId)
                    .contentType(MediaType.parseMediaType("text/markdown;charset=UTF-8"))
                    .body(markdownReply);
        }

        // Standard JSON response containing Markdown in the reply field and the active chatId
        ChatResponseDto responseDto = new ChatResponseDto(
                markdownReply,
                List.of("semanticProductSearch", "getCustomerHistory", "searchInManuals"),
                Collections.emptyList(),
                effectiveChatId
        );

        return ResponseEntity.ok()
                .header("X-Chat-Id", effectiveChatId)
                .body(responseDto);
    }
}
