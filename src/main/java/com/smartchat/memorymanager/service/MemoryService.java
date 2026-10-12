package com.smartchat.memorymanager.service;

import com.smartchat.memorymanager.dto.MemoryRequest;
import com.smartchat.memorymanager.model.Memory;
import com.smartchat.memorymanager.model.MemoryType;
import com.smartchat.memorymanager.repository.MemoryRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class MemoryService {
    private final MemoryRepository repository;

    public MemoryService(MemoryRepository repository) {
        this.repository = repository;
    }

    public List<Memory> getAll() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    public Memory add(MemoryRequest request) {
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new IllegalArgumentException("Memory content cannot be empty");
        }
        MemoryType type = request.getType() == null ? MemoryType.SEMANTIC : request.getType();
        double importance = request.getImportance() == null ? 1.0 : Math.max(0.0, Math.min(5.0, request.getImportance()));
        return repository.save(new Memory(type, request.getContent().trim(), importance));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
