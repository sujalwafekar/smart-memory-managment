package com.smartchat.memorymanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartchat.memorymanager.dto.ExtractedMemory;
import com.smartchat.memorymanager.dto.ScoredMemory;
import com.smartchat.memorymanager.model.MemoryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Service
@Primary
public class NvidiaAiService implements AiService {

    private static final Logger log = LoggerFactory.getLogger(NvidiaAiService.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final MockAiService fallbackMockService;
    private final String apiUrl;
    private final List<ModelTarget> fallbackChain;

    public NvidiaAiService(
            ObjectMapper objectMapper,
            MockAiService fallbackMockService,
            @Value("${nvidia.api.url}") String apiUrl,
            @Value("${nvidia.api.key.kimi}") String keyKimi,
            @Value("${nvidia.api.key.nemotron}") String keyNemotron
    ) {
        this.objectMapper = objectMapper;
        this.fallbackMockService = fallbackMockService;
        this.apiUrl = apiUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        // 3-Model fallback chain:
        // 1. moonshotai/kimi-k3
        // 2. nvidia/nemotron-3.5-lightning-30b-a3b
        // 3. nvidia/nemotron-3-ultra-550b-a55b
        this.fallbackChain = List.of(
            new ModelTarget(
                "moonshotai/kimi-k3",
                keyKimi,
                Map.of(
                    "temperature", 1.0,
                    "max_tokens", 16384,
                    "seed", 0,
                    "reasoning_effort", "max"
                )
            ),
            new ModelTarget(
                "nvidia/nemotron-3.5-lightning-30b-a3b",
                keyNemotron,
                Map.of(
                    "temperature", 1.0,
                    "top_p", 0.95,
                    "max_tokens", 16384,
                    "chat_template_kwargs", Map.of("enable_thinking", true),
                    "reasoning_budget", 16384
                )
            ),
            new ModelTarget(
                "nvidia/nemotron-3-ultra-550b-a55b",
                keyNemotron,
                Map.of(
                    "temperature", 1.0,
                    "top_p", 0.95,
                    "max_tokens", 16384,
                    "chat_template_kwargs", Map.of("enable_thinking", true)
                )
            )
        );
    }

    @Override
    public String generate(String question, List<ScoredMemory> selectedMemories, String prompt) {
        List<Map<String, String>> messages = List.of(
            Map.of("role", "system", "content", "You are SmartChat, an intelligent AI assistant. Use the provided user context to tailor your response."),
            Map.of("role", "user", "content", prompt)
        );

        List<String> errors = new ArrayList<>();

        for (int i = 0; i < fallbackChain.size(); i++) {
            ModelTarget target = fallbackChain.get(i);
            log.info("Querying NVIDIA model [{}/{}]: {}", i + 1, fallbackChain.size(), target.model());

            try {
                Map<String, Object> body = new HashMap<>(target.params());
                body.put("model", target.model());
                body.put("messages", messages);
                body.put("stream", false);

                String requestJson = objectMapper.writeValueAsString(body);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl))
                        .timeout(Duration.ofSeconds(20))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + target.apiKey())
                        .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonNode root = objectMapper.readTree(response.body());
                    JsonNode choices = root.path("choices");
                    if (choices.isArray() && !choices.isEmpty()) {
                        String answer = choices.get(0).path("message").path("content").asText();
                        log.info("Successfully received answer from {}", target.model());
                        return answer;
                    }
                } else {
                    String err = "HTTP " + response.statusCode() + " from " + target.model() + ": " + response.body();
                    log.warn(err);
                    errors.add(err);
                }
            } catch (Exception e) {
                String err = target.model() + " exception: " + e.getMessage();
                log.warn(err);
                errors.add(err);
            }
        }

        // If all 3 cloud models fail (offline / invalid keys / rate limit), seamlessly fall back to local mock service
        log.warn("All NVIDIA models failed. Falling back to internal mock AI service. Encountered: {}", errors);
        return fallbackMockService.generate(question, selectedMemories, prompt);
    }

    @Override
    public List<ExtractedMemory> extractMemories(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) return List.of();

        String systemPrompt = """
            You are a cognitive memory extraction system.
            Analyze the user's message to identify if they shared any long-term personal facts, tech stack choices, preferences, or completed milestones.
            Rules:
            1. If the user states a personal preference, technical choice, decision, or fact about themselves/their project (e.g., "I decided to use PostgreSQL instead of MySQL", "I love dark mode", "We use Docker"): classify as type "SEMANTIC", importance between 1.0 and 5.0, with content written in third person starting with "User ...".
            2. If the user states a completed achievement, milestone, or past event (e.g., "I finished the auth module", "I participated in a hackathon"): classify as type "EPISODIC", importance between 1.0 and 5.0, with content written in third person starting with "User ...".
            3. If the user message is just a general question, greeting, or contains NO facts about the user/project (e.g., "What is a database?", "How do I invert a binary tree?"): return an empty JSON array [].
            4. Output STRICTLY a valid JSON array of objects with keys: "type" (string: "SEMANTIC" or "EPISODIC"), "content" (string), "importance" (number). Do NOT include markdown code blocks, explanations, or any extra text.
            """;

        List<Map<String, String>> messages = List.of(
            Map.of("role", "system", "content", systemPrompt),
            Map.of("role", "user", "content", userMessage)
        );

        for (int i = 0; i < fallbackChain.size(); i++) {
            ModelTarget target = fallbackChain.get(i);
            try {
                Map<String, Object> body = new HashMap<>(target.params());
                body.put("model", target.model());
                body.put("messages", messages);
                body.put("stream", false);
                body.put("max_tokens", 500);
                body.put("temperature", 0.1);
                body.put("chat_template_kwargs", Map.of("enable_thinking", false));

                String requestJson = objectMapper.writeValueAsString(body);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl))
                        .timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + target.apiKey())
                        .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonNode root = objectMapper.readTree(response.body());
                    JsonNode choices = root.path("choices");
                    if (choices.isArray() && !choices.isEmpty()) {
                        String rawContent = choices.get(0).path("message").path("content").asText();
                        List<ExtractedMemory> parsed = parseExtractedMemories(rawContent);
                        log.info("Extracted {} memories via {}", parsed.size(), target.model());
                        if (!parsed.isEmpty()) {
                            return parsed;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Memory extraction failed on {}: {}", target.model(), e.getMessage());
            }
        }

        // Fallback to internal heuristic extractor
        return fallbackMockService.extractMemories(userMessage);
    }

    private List<ExtractedMemory> parseExtractedMemories(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) return List.of();
        int start = rawJson.indexOf('[');
        int end = rawJson.lastIndexOf(']');
        if (start == -1 || end == -1 || end <= start) {
            return List.of();
        }
        String json = rawJson.substring(start, end + 1).trim();

        try {
            JsonNode arrayNode = objectMapper.readTree(json);
            if (arrayNode.isArray()) {
                List<ExtractedMemory> result = new ArrayList<>();
                for (JsonNode item : arrayNode) {
                    String typeStr = item.path("type").asText("SEMANTIC").toUpperCase(Locale.ROOT);
                    MemoryType type = "EPISODIC".equalsIgnoreCase(typeStr) ? MemoryType.EPISODIC : MemoryType.SEMANTIC;
                    String content = item.path("content").asText();
                    double importance = item.path("importance").asDouble(2.5);
                    if (content != null && !content.isBlank()) {
                        result.add(new ExtractedMemory(type, content.trim(), importance));
                    }
                }
                return result;
            }
        } catch (Exception ex) {
            log.warn("Failed to parse extracted memories JSON: '{}', error: {}", json, ex.getMessage());
        }
        return List.of();
    }

    private record ModelTarget(String model, String apiKey, Map<String, Object> params) {}
}

