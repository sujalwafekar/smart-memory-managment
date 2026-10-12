package com.smartchat.memorymanager.service;

import com.smartchat.memorymanager.dto.ExtractedMemory;
import com.smartchat.memorymanager.dto.ScoredMemory;
import java.util.List;

public interface AiService {
    String generate(String question, List<ScoredMemory> selectedMemories, String prompt);
    List<ExtractedMemory> extractMemories(String userMessage);
}
