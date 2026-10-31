package com.smartchat.memorymanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartchat.memorymanager.dto.ExtractedMemory;
import com.smartchat.memorymanager.dto.ScoredMemory;
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
import java.util.List;
import java.util.Map;

/** Uses Gemini for chat answers and NVIDIA only when Gemini is unavailable. */
@Service
@Primary
public class GeminiAiService implements AiService {
    private static final Logger log = LoggerFactory.getLogger(GeminiAiService.class);
    private final ObjectMapper objectMapper;
    private final NvidiaAiService nvidiaAiService;
    private final MockAiService localMemoryExtractor;
    private final HttpClient httpClient;
    private final String apiUrl;
    private final String apiKey;

    public GeminiAiService(
            ObjectMapper objectMapper,
            NvidiaAiService nvidiaAiService,
            MockAiService localMemoryExtractor,
            @Value("${gemini.api.url}") String apiUrl,
            @Value("${gemini.api.key}") String apiKey
    ) {
        this.objectMapper = objectMapper;
        this.nvidiaAiService = nvidiaAiService;
        this.localMemoryExtractor = localMemoryExtractor;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Override
    public String generate(String question, List<ScoredMemory> selectedMemories, String prompt) {
        if (!apiKey.isBlank()) {
            try {
                Map<String, Object> body = Map.of(
                        "systemInstruction", Map.of("parts", List.of(Map.of("text", "You are MemoryChat. Give a direct, useful answer in plain text. Do not use Markdown headings, bold markers, backticks, code fences, source code, or code comments. Use relevant saved context quietly; never mention memory selection unless the user asks."))),
                        "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))),
                        "generationConfig", Map.of("temperature", 0.7, "maxOutputTokens", 1024)
                );
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl))
                        .timeout(Duration.ofSeconds(12))
                        .header("Content-Type", "application/json")
                        .header("x-goog-api-key", apiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    JsonNode parts = objectMapper.readTree(response.body()).path("candidates").path(0).path("content").path("parts");
                    if (parts.isArray() && !parts.isEmpty()) {
                        String answer = parts.get(0).path("text").asText().trim();
                        if (!answer.isBlank()) return answer;
                    }
                }
                log.warn("Gemini returned HTTP {}", response.statusCode());
            } catch (Exception exception) {
                log.warn("Gemini request failed: {}", exception.getMessage());
            }
        } else {
            log.warn("Gemini API key is not configured.");
        }

        return nvidiaAiService.generate(question, selectedMemories, prompt);
    }

    @Override
    public List<ExtractedMemory> extractMemories(String userMessage) {
        return localMemoryExtractor.extractMemories(userMessage);
    }
}
