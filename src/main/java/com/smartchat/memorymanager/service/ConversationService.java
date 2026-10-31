package com.smartchat.memorymanager.service;

import com.smartchat.memorymanager.dto.*;
import com.smartchat.memorymanager.model.*;
import com.smartchat.memorymanager.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConversationService {
    private final ConversationRepository conversations;
    private final ChatMessageRepository messages;

    public ConversationService(ConversationRepository conversations, ChatMessageRepository messages) {
        this.conversations = conversations;
        this.messages = messages;
    }

    public Conversation getOrCreate(Long id, AppUser user) {
        if (id == null) return conversations.save(new Conversation(user));
        return conversations.findByIdAndOwnerUsername(id, user.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
    }

    @Transactional
    public void addMessage(Conversation conversation, String role, String content) {
        if ("user".equals(role) && "New conversation".equals(conversation.getTitle())) {
            conversation.setTitle(makeTitle(content));
        }
        conversation.setUpdatedAt(LocalDateTime.now());
        messages.save(new ChatMessage(conversation, role, content));
    }

    public List<ConversationSummary> list(AppUser user) {
        return conversations.findAllByOwnerUsernameOrderByUpdatedAtDesc(user.getUsername()).stream()
                .map(item -> new ConversationSummary(item.getId(), item.getTitle(), item.getUpdatedAt())).toList();
    }

    public ConversationDetail get(Long id, AppUser user) {
        Conversation conversation = getOrCreate(id, user);
        List<ConversationMessage> history = messages.findAllByConversationIdOrderByCreatedAtAsc(conversation.getId()).stream()
                .map(item -> new ConversationMessage(item.getRole(), item.getContent(), item.getCreatedAt())).toList();
        return new ConversationDetail(conversation.getId(), conversation.getTitle(), history);
    }

    private String makeTitle(String content) {
        String compact = content.trim().replaceAll("\\s+", " ");
        return compact.length() <= 58 ? compact : compact.substring(0, 55) + "...";
    }
}
