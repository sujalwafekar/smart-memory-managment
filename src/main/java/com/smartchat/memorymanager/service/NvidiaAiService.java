package com.smartchat.memorymanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartchat.memorymanager.dto.ExtractedMemory;
import com.smartchat.memorymanager.dto.ScoredMemory;
import com.smartchat.memorymanager.model.MemoryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Service
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
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        // Keep the interactive chat responsive. A slow cloud provider should not
        // make the whole application wait while the local demo AI is available.
        this.fallbackChain = List.of(
            new ModelTarget(
                "nvidia/nemotron-3.5-lightning-30b-a3b",
                keyKimi,
                Map.of(
                    "temperature", 0.7,
                    "max_tokens", 1024,
                    "chat_template_kwargs", Map.of("enable_thinking", false)
                )
            )
        );
    }

    @Override
    public String generate(String question, List<ScoredMemory> selectedMemories, String prompt) {
        List<Map<String, String>> messages = List.of(
            Map.of("role", "system", "content", "You are MemoryChat, an intelligent AI assistant. Use the provided user context to tailor your response."),
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
                        .timeout(Duration.ofSeconds(8))
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

        log.warn("NVIDIA could not generate an answer. Encountered: {}", errors);
        throw new IllegalStateException("NVIDIA could not generate an answer.");
    }

    @Override
    public List<ExtractedMemory> extractMemories(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) return List.of();
        // Extraction happens locally so saving a memory never delays the answer.
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
