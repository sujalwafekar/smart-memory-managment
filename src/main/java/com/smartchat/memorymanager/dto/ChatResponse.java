package com.smartchat.memorymanager.dto;

import java.util.List;

public record ChatResponse(
        String answer,
        List<String> keywords,
        List<ScoredMemory> relevantMemories,
        List<String> learnedMemories
) {
    public ChatResponse(String answer, List<String> keywords, List<ScoredMemory> relevantMemories) {
        this(answer, keywords, relevantMemories, List.of());
    }
}
