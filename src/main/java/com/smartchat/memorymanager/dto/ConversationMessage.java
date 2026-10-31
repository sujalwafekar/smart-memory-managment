package com.smartchat.memorymanager.dto;
import java.time.LocalDateTime;
public record ConversationMessage(String role, String content, LocalDateTime createdAt) { }
