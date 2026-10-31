package com.smartchat.memorymanager.controller;

import com.smartchat.memorymanager.dto.*;
import com.smartchat.memorymanager.model.AppUser;
import com.smartchat.memorymanager.service.ConversationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {
    private final ConversationService service;
    public ConversationController(ConversationService service) { this.service = service; }

    @GetMapping
    public List<ConversationSummary> list(Authentication authentication) {
        return service.list((AppUser) authentication.getPrincipal());
    }

    @GetMapping("/{id}")
    public ConversationDetail get(@PathVariable Long id, Authentication authentication) {
        return service.get(id, (AppUser) authentication.getPrincipal());
    }
}
