package com.smartchat.memorymanager.service;

import com.smartchat.memorymanager.dto.ExtractedMemory;
import com.smartchat.memorymanager.dto.ScoredMemory;
import com.smartchat.memorymanager.model.MemoryType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class MockAiService implements AiService {
    @Override
    public String generate(String question, List<ScoredMemory> selectedMemories, String prompt) {
        String q = question.toLowerCase(Locale.ROOT);
        String context = selectedMemories.stream().map(ScoredMemory::content).reduce("", (a,b) -> a + " " + b).toLowerCase(Locale.ROOT);

        if (q.contains("auth") || q.contains("login")) {
            if (context.contains("firebase")) {
                return "Because your stored context says the project uses Firebase and you already worked on the login module, the next logical step is Firebase Authentication: enable your required sign-in provider, connect the existing login form, keep the authenticated user state, and protect private pages. SmartChat selected only the memories related to authentication instead of sending the complete history.";
            }
            return "For authentication, first choose a provider, connect the login form to the backend, create a secure session/token flow, and protect private routes. Add your project stack as a semantic memory so SmartChat can tailor the answer.";
        }

        if (q.contains("memory") || q.contains("relevant")) {
            return "SmartChat extracts keywords from the current question, scores stored memories using memory-type weight plus keyword/synonym matches, selects the top relevant items, and sends only those memories as context to the AI layer.";
        }

        if (!selectedMemories.isEmpty()) {
            return "I found " + selectedMemories.size() + " relevant stored memor" + (selectedMemories.size() == 1 ? "y" : "ies") + " and used them as context. The backend explainer shows exactly which memories were selected and why.";
        }

        return "I could not find a strongly relevant stored memory for that question. I can still answer, but this demo intentionally avoids injecting unrelated context.";
    }

    @Override
    public List<ExtractedMemory> extractMemories(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) return List.of();
        String lower = userMessage.toLowerCase(Locale.ROOT);
        List<ExtractedMemory> list = new ArrayList<>();

        // Detect episodic milestones (events completed)
        if (lower.contains("finished") || lower.contains("completed") || lower.contains("deployed") || lower.contains("participated in") || lower.contains("attended")) {
            String sanitized = formatAsFact(userMessage);
            list.add(new ExtractedMemory(MemoryType.EPISODIC, sanitized, 2.0));
        }
        // Detect semantic decisions/preferences/tech stack
        else if (lower.contains("decided to") || lower.contains("switched to") || lower.contains("prefer") || lower.contains("using") || lower.contains("stack is") || lower.contains("i know ") || lower.contains("we use")) {
            String sanitized = formatAsFact(userMessage);
            list.add(new ExtractedMemory(MemoryType.SEMANTIC, sanitized, 2.5));
        }

        return list;
    }

    private String formatAsFact(String text) {
        String trimmed = text.trim();
        if (trimmed.toLowerCase(Locale.ROOT).startsWith("i ")) {
            return "User " + trimmed.substring(2);
        } else if (trimmed.toLowerCase(Locale.ROOT).startsWith("we ")) {
            return "User and team " + trimmed.substring(3);
        }
        return "User: " + trimmed;
    }
}
