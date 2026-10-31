package com.smartchat.memorymanager.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 12) private String role;
    @Column(nullable = false, columnDefinition = "TEXT") private String content;
    @Column(nullable = false) private LocalDateTime createdAt = LocalDateTime.now();
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "conversation_id", nullable = false) @JsonIgnore private Conversation conversation;
    public ChatMessage() { }
    public ChatMessage(Conversation conversation, String role, String content) { this.conversation = conversation; this.role = role; this.content = content; }
    public String getRole() { return role; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
