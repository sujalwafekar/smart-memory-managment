package com.smartchat.memorymanager.dto;
import java.time.LocalDateTime;
public record ConversationSummary(Long id, String title, LocalDateTime updatedAt) { }
