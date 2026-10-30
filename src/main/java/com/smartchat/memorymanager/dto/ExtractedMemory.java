package com.smartchat.memorymanager.dto;

import com.smartchat.memorymanager.model.MemoryType;

public record ExtractedMemory(
        MemoryType type,
        String content,
        Double importance
) {}

