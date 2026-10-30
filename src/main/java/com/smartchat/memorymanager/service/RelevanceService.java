package com.smartchat.memorymanager.service;

import com.smartchat.memorymanager.dto.ScoredMemory;
import com.smartchat.memorymanager.model.Memory;
import com.smartchat.memorymanager.model.MemoryType;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RelevanceService {
    private static final Set<String> STOP_WORDS = Set.of(
            "the","a","an","is","are","am","i","me","my","we","our","you","your","to","of","in","on","for",
            "and","or","but","how","what","when","where","why","can","could","should","would","do","does","did",
            "this","that","it","with","from","into","about","please","add","make","implement"
    );

    private static final Map<String, Set<String>> SYNONYMS = Map.ofEntries(
            Map.entry("authentication", Set.of("auth", "login", "signin", "firebase")),
            Map.entry("login", Set.of("authentication", "auth", "signin", "firebase")),
            Map.entry("database", Set.of("mysql", "sql", "storage", "db")),
            Map.entry("frontend", Set.of("html", "css", "javascript", "ui")),
            Map.entry("backend", Set.of("spring", "springboot", "api", "server", "java")),
            Map.entry("memory", Set.of("remember", "context", "semantic", "episodic", "working")),
            Map.entry("react", Set.of("frontend", "javascript")),
            Map.entry("firebase", Set.of("authentication", "auth", "login"))
    );

    public List<String> extractKeywords(String question) {
        if (question == null) return List.of();
        LinkedHashSet<String> words = Arrays.stream(question.toLowerCase(Locale.ROOT).split("[^a-z0-9+#.-]+"))
                .map(String::trim)
                .filter(w -> w.length() > 2)
                .filter(w -> !STOP_WORDS.contains(w))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new ArrayList<>(words);
    }

    public List<ScoredMemory> score(String question, List<Memory> memories, int limit) {
        List<String> keywords = extractKeywords(question);
        List<ScoreHolder> holders = new ArrayList<>();

        for (Memory memory : memories) {
            String content = memory.getContent().toLowerCase(Locale.ROOT);
            double score = typeWeight(memory.getType()) + (memory.getImportance() == null ? 1.0 : memory.getImportance());
            LinkedHashSet<String> matched = new LinkedHashSet<>();

            for (String keyword : keywords) {
                if (content.contains(keyword)) {
                    score += 3.0;
                    matched.add(keyword);
                }
                Set<String> related = SYNONYMS.getOrDefault(keyword, Set.of());
                for (String synonym : related) {
                    if (content.contains(synonym)) {
                        score += 1.25;
                        matched.add(keyword + "~" + synonym);
                        break;
                    }
                }
            }

            if (!matched.isEmpty()) score += 1.0;
            holders.add(new ScoreHolder(memory, score, new ArrayList<>(matched)));
        }

        holders.sort(Comparator.comparingDouble(ScoreHolder::score).reversed());
        double threshold = keywords.isEmpty() ? 4.0 : 5.0;
        Set<Long> selectedIds = holders.stream()
                .filter(h -> !h.matches().isEmpty() && h.score() >= threshold)
                .limit(limit)
                .map(h -> h.memory().getId())
                .collect(Collectors.toSet());

        return holders.stream()
                .map(h -> new ScoredMemory(
                        h.memory().getId(),
                        h.memory().getType(),
                        h.memory().getContent(),
                        Math.round(h.score() * 100.0) / 100.0,
                        h.matches(),
                        selectedIds.contains(h.memory().getId())
                ))
                .toList();
    }

    private double typeWeight(MemoryType type) {
        return switch (type) {
            case WORKING -> 3.0;
            case SEMANTIC -> 2.0;
            case EPISODIC -> 1.0;
            case ARCHIVE -> 0.5;
        };
    }

    private record ScoreHolder(Memory memory, double score, List<String> matches) {}
}
