package com.smartchat.memorymanager.service;

import com.smartchat.memorymanager.dto.*;
import com.smartchat.memorymanager.model.Memory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatService {
    private final MemoryService memoryService;
    private final RelevanceService relevanceService;
    private final AiService aiService;
    private final TraceService traceService;

    public ChatService(MemoryService memoryService,
                       RelevanceService relevanceService,
                       AiService aiService,
                       TraceService traceService) {
        this.memoryService = memoryService;
        this.relevanceService = relevanceService;
        this.aiService = aiService;
        this.traceService = traceService;
    }

    public ChatResponse chat(String question) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("Message cannot be empty");
        }

        // 1. AI-Driven memory extraction (detect facts, tech stack decisions, preferences, milestones)
        List<ExtractedMemory> extracted = aiService.extractMemories(question);
        List<Memory> existingMemories = memoryService.getAll();
        List<String> learnedMemories = new java.util.ArrayList<>();

        for (ExtractedMemory item : extracted) {
            if (item.content() == null || item.content().isBlank()) continue;
            boolean alreadyExists = existingMemories.stream()
                    .anyMatch(m -> m.getContent().trim().equalsIgnoreCase(item.content().trim()));
            if (!alreadyExists) {
                Memory saved = memoryService.add(new MemoryRequest(item.type(), item.content().trim(), item.importance()));
                learnedMemories.add("[" + saved.getType() + "] " + saved.getContent());
            }
        }

        // Refresh memory pool if any new memories were saved
        List<Memory> memories = learnedMemories.isEmpty() ? existingMemories : memoryService.getAll();
        List<String> keywords = relevanceService.extractKeywords(question);
        List<ScoredMemory> scored = relevanceService.score(question, memories, 5);
        List<ScoredMemory> selected = scored.stream().filter(ScoredMemory::selected).toList();
        String prompt = buildPrompt(question, selected);
        String answer = aiService.generate(question, selected, prompt);

        traceService.setLatest(new TraceResponse(
                LocalDateTime.now(), question, keywords, scored, selected, prompt, answer, learnedMemories
        ));

        return new ChatResponse(answer, keywords, selected, learnedMemories);
    }

    private String buildPrompt(String question, List<ScoredMemory> selected) {
        StringBuilder prompt = new StringBuilder("You are SmartChat, a helpful assistant.\n\nRelevant user memory:\n");
        if (selected.isEmpty()) {
            prompt.append("- No strongly relevant memory found.\n");
        } else {
            for (ScoredMemory memory : selected) {
                prompt.append("- [").append(memory.type()).append("] ").append(memory.content()).append("\n");
            }
        }
        prompt.append("\nCurrent question:\n").append(question);
        return prompt.toString();
    }
}
