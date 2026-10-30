package com.smartchat.memorymanager.service;

import com.smartchat.memorymanager.dto.MemoryRequest;
import com.smartchat.memorymanager.model.Memory;
import com.smartchat.memorymanager.model.MemoryType;
import com.smartchat.memorymanager.model.AppUser;
import com.smartchat.memorymanager.repository.MemoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class MemoryService {
    private final MemoryRepository repository;

    public MemoryService(MemoryRepository repository) {
        this.repository = repository;
    }

    public List<Memory> getAll(String username) {
        return repository.findAllByOwnerUsernameOrderByCreatedAtDesc(username);
    }

    public Memory add(MemoryRequest request, AppUser owner) {
        validate(request);
        MemoryType type = memoryType(request);
        double importance = importance(request);
        return repository.save(new Memory(type, request.getContent().trim(), importance, owner));
    }

    @Transactional
    public Optional<Memory> update(Long id, MemoryRequest request, String username) {
        validate(request);
        return repository.findByIdAndOwnerUsername(id, username).map(memory -> {
            memory.setType(memoryType(request));
            memory.setContent(request.getContent().trim());
            memory.setImportance(importance(request));
            return memory;
        });
    }

    @Transactional
    public boolean delete(Long id, String username) {
        return repository.deleteByIdAndOwnerUsername(id, username) > 0;
    }

    private void validate(MemoryRequest request) {
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new IllegalArgumentException("Memory content cannot be empty");
        }
    }

    private MemoryType memoryType(MemoryRequest request) {
        return request.getType() == null ? MemoryType.SEMANTIC : request.getType();
    }

    private double importance(MemoryRequest request) {
        return request.getImportance() == null ? 1.0 : Math.max(0.0, Math.min(5.0, request.getImportance()));
    }
}
