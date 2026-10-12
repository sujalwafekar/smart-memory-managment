package com.smartchat.memorymanager.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TraceResponse(
        LocalDateTime timestamp,
        String question,
        List<String> keywords,
        List<ScoredMemory> scoredMemories,
        List<ScoredMemory> selectedMemories,
        String prompt,
        String answer,
        List<String> learnedMemories
) {
    public TraceResponse(LocalDateTime timestamp, String question, List<String> keywords,
                         List<ScoredMemory> scoredMemories, List<ScoredMemory> selectedMemories,
                         String prompt, String answer) {
        this(timestamp, question, keywords, scoredMemories, selectedMemories, prompt, answer, List.of());
    }
}
