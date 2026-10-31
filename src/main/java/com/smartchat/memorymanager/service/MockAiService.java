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
                return "Because your stored context says the project uses Firebase and you already worked on the login module, the next logical step is Firebase Authentication: enable your required sign-in provider, connect the existing login form, keep the authenticated user state, and protect private pages. MemoryChat selected only the memories related to authentication instead of sending the complete history.";
            }
            return "For authentication, first choose a provider, connect the login form to the backend, create a secure session/token flow, and protect private routes. Add your project stack as a semantic memory so MemoryChat can tailor the answer.";
        }

        if (q.contains("memory") || q.contains("relevant")) {
            return "MemoryChat extracts keywords from the current question, scores stored memories using memory-type weight plus keyword/synonym matches, selects the top relevant items, and sends only those memories as context to the AI layer.";
        }

        if (!selectedMemories.isEmpty()) {
            return "I found " + selectedMemories.size() + " relevant stored memor" + (selectedMemories.size() == 1 ? "y" : "ies") + " and used them as context. The backend explainer shows exactly which memories were selected and why.";
        }

        return "I could not find a strongly relevant stored memory for that question. I can still answer, but this demo intentionally avoids injecting unrelated context.";
    }

    @Override
    public List<ExtractedMemory> extractMemories(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) return List.of();
        String candidate = userMessage.trim();
        String lower = candidate.toLowerCase(Locale.ROOT);
        List<ExtractedMemory> list = new ArrayList<>();

        // Do not turn ordinary questions into saved facts unless the user explicitly asks us to remember one.
        boolean explicitlyRemembered = lower.startsWith("remember ") || lower.startsWith("please remember ") || lower.startsWith("save ");
        if (candidate.endsWith("?") && !explicitlyRemembered) return List.of();

        if (explicitlyRemembered) {
            String fact = candidate.replaceFirst("(?i)^(please\\s+)?(remember|save)(\\s+that)?\\s+", "");
            if (!fact.isBlank()) {
                list.add(new ExtractedMemory(classifyMemoryType(fact.toLowerCase(Locale.ROOT)), formatAsFact(fact), 3.0));
            }
            return list;
        }

        // Detect episodic milestones (events completed).
        if (lower.contains("finished") || lower.contains("completed") || lower.contains("deployed") || lower.contains("participated in") || lower.contains("attended")) {
            String sanitized = formatAsFact(candidate);
            list.add(new ExtractedMemory(MemoryType.EPISODIC, sanitized, 2.0));
        }
        // Detect project facts, preferences, technology choices, and profile details.
        else if (lower.contains("decided to") || lower.contains("switched to") || lower.contains("prefer") || lower.contains("using") || lower.contains("stack is") || lower.contains("i know ") || lower.contains("we use") || lower.contains("i use") || lower.contains("my project") || lower.contains("our project") || lower.contains("my name is") || lower.startsWith("i am ") || lower.startsWith("i'm ") || lower.startsWith("we are ")) {
            list.add(new ExtractedMemory(MemoryType.SEMANTIC, formatAsFact(candidate), 2.5));
        }
        // Detect active tasks that are useful in the current conversation but may change later.
        else if (lower.contains("working on") || lower.contains("building ") || lower.contains("planning to") || lower.contains("we need") || lower.contains("i need")) {
            list.add(new ExtractedMemory(MemoryType.WORKING, formatAsFact(candidate), 2.0));
        }

        return list;
    }

    private MemoryType classifyMemoryType(String fact) {
        if (fact.contains("finished") || fact.contains("completed") || fact.contains("deployed")) return MemoryType.EPISODIC;
        if (fact.contains("working on") || fact.contains("building") || fact.contains("need") || fact.contains("plan")) return MemoryType.WORKING;
        return MemoryType.SEMANTIC;
    }

    private String formatAsFact(String text) {
        String trimmed = text.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("i am ")) {
            return "User is " + trimmed.substring(5);
        } else if (lower.startsWith("i'm ")) {
            return "User is " + trimmed.substring(4);
        } else if (lower.startsWith("i ")) {
            return "User " + trimmed.substring(2);
        } else if (lower.startsWith("we are ")) {
            return "User and team are " + trimmed.substring(7);
        } else if (lower.startsWith("we ")) {
            return "User and team " + trimmed.substring(3);
        }
        return "User: " + trimmed;
    }
}
