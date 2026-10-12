package com.smartchat.memorymanager.repository;

import com.smartchat.memorymanager.model.Memory;
import com.smartchat.memorymanager.model.MemoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MemoryRepository extends JpaRepository<Memory, Long> {
    List<Memory> findAllByOrderByCreatedAtDesc();
    List<Memory> findByTypeOrderByCreatedAtDesc(MemoryType type);
}
