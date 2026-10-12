package com.smartchat.memorymanager.dto;

import com.smartchat.memorymanager.model.MemoryType;
import java.util.List;

public record ScoredMemory(
        Long id,
        MemoryType type,
        String content,
        double score,
        List<String> matchedKeywords,
        boolean selected
) {}
