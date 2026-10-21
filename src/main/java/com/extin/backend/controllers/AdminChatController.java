package com.extin.backend.controllers;

import com.extin.backend.models.dtos.ChatRequestDto;
import com.extin.backend.models.dtos.ChatResponseDto;
import com.extin.backend.services.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin REST Controller for MCP Chatbot Assistant.
 * Endpoint: POST /api/admin/chat
 */
@RestController
@RequestMapping("/api/admin/chat")
public class AdminChatController {

    private final ChatService chatService;

    public AdminChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Sends a natural language prompt to the Spring Boot MCP Server.
     */
    @PostMapping
    public ResponseEntity<ChatResponseDto> chat(@Valid @RequestBody ChatRequestDto requestDto) {
        ChatResponseDto response = chatService.processChat(requestDto);
        return ResponseEntity.ok(response);
    }
}
