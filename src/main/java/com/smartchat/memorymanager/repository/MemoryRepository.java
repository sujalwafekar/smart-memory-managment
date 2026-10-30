package com.smartchat.memorymanager.repository;

import com.smartchat.memorymanager.model.Memory;
import com.smartchat.memorymanager.model.MemoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MemoryRepository extends JpaRepository<Memory, Long> {
    List<Memory> findAllByOwnerUsernameOrderByCreatedAtDesc(String username);
    List<Memory> findByOwnerUsernameAndTypeOrderByCreatedAtDesc(String username, MemoryType type);
    Optional<Memory> findByIdAndOwnerUsername(Long id, String username);
    long deleteByIdAndOwnerUsername(Long id, String username);
}
