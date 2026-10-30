package com.smartchat.memorymanager.controller;

import com.smartchat.memorymanager.dto.MemoryRequest;
import com.smartchat.memorymanager.model.Memory;
import com.smartchat.memorymanager.service.MemoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.util.List;

@RestController
@RequestMapping("/api/memories")
public class MemoryController {
    private final MemoryService memoryService;

    public MemoryController(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @GetMapping
    public List<Memory> getAll(Authentication authentication) {
        return memoryService.getAll(authentication.getName());
    }

    @PostMapping
    public ResponseEntity<Memory> add(@RequestBody MemoryRequest request, Authentication authentication) {
        return ResponseEntity.ok(memoryService.add(request, (com.smartchat.memorymanager.model.AppUser) authentication.getPrincipal()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Memory> update(@PathVariable Long id, @RequestBody MemoryRequest request, Authentication authentication) {
        return memoryService.update(id, request, authentication.getName())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        return memoryService.delete(id, authentication.getName()) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
