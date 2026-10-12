package com.smartchat.memorymanager.controller;

import com.smartchat.memorymanager.dto.MemoryRequest;
import com.smartchat.memorymanager.model.Memory;
import com.smartchat.memorymanager.service.MemoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/memories")
public class MemoryController {
    private final MemoryService memoryService;

    public MemoryController(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @GetMapping
    public List<Memory> getAll() {
        return memoryService.getAll();
    }

    @PostMapping
    public ResponseEntity<Memory> add(@RequestBody MemoryRequest request) {
        return ResponseEntity.ok(memoryService.add(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        memoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
