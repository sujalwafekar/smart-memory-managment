package com.smartchat.memorymanager.controller;

import com.smartchat.memorymanager.dto.ChatRequest;
import com.smartchat.memorymanager.dto.ChatResponse;
import com.smartchat.memorymanager.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        return ResponseEntity.ok(chatService.chat(request.getMessage()));
    }
}
