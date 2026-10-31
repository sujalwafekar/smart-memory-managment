package com.smartchat.memorymanager.service;

import com.smartchat.memorymanager.dto.*;
import com.smartchat.memorymanager.model.Memory;
import com.smartchat.memorymanager.model.AppUser;
import com.smartchat.memorymanager.model.Conversation;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatService {
    private final MemoryService memoryService;
    private final RelevanceService relevanceService;
    private final AiService aiService;
    private final ConversationService conversationService;

    public ChatService(MemoryService memoryService,
                       RelevanceService relevanceService,
                       AiService aiService,
                       ConversationService conversationService) {
        this.memoryService = memoryService;
        this.relevanceService = relevanceService;
        this.aiService = aiService;
        this.conversationService = conversationService;
    }

    public ChatResponse chat(String question, Long conversationId, AppUser user) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("Message cannot be empty");
        }

        Conversation conversation = conversationService.getOrCreate(conversationId, user);
        conversationService.addMessage(conversation, "user", question.trim());

        // 1. AI-Driven memory extraction (detect facts, tech stack decisions, preferences, milestones)
        List<ExtractedMemory> extracted = aiService.extractMemories(question);
        List<Memory> existingMemories = memoryService.getAll(user.getUsername());
        List<String> learnedMemories = new java.util.ArrayList<>();

        for (ExtractedMemory item : extracted) {
            if (item.content() == null || item.content().isBlank()) continue;
            boolean alreadyExists = existingMemories.stream()
                    .anyMatch(m -> m.getContent().trim().equalsIgnoreCase(item.content().trim()));
            if (!alreadyExists) {
                Memory saved = memoryService.add(new MemoryRequest(item.type(), item.content().trim(), item.importance()), user);
                learnedMemories.add("[" + saved.getType() + "] " + saved.getContent());
            }
        }

        // Refresh memory pool if any new memories were saved
        List<Memory> memories = learnedMemories.isEmpty() ? existingMemories : memoryService.getAll(user.getUsername());
        List<String> keywords = relevanceService.extractKeywords(question);
        List<ScoredMemory> scored = relevanceService.score(question, memories, 5);
        List<ScoredMemory> selected = scored.stream().filter(ScoredMemory::selected).toList();
        String prompt = buildPrompt(question, selected);
        String answer = aiService.generate(question, selected, prompt);
        conversationService.addMessage(conversation, "assistant", answer);

        return new ChatResponse(answer, keywords, selected, learnedMemories, conversation.getId());
    }

    private String buildPrompt(String question, List<ScoredMemory> selected) {
        StringBuilder prompt = new StringBuilder("You are MemoryChat, a helpful assistant.\n\nRelevant user memory:\n");
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
