package com.smartchat.memorymanager.dto;
import java.util.List;
public record ConversationDetail(Long id, String title, List<ConversationMessage> messages) { }
