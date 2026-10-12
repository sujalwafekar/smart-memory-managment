package com.smartchat.memorymanager.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "memories")
public class Memory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemoryType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Double importance = 1.0;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Memory() {}

    public Memory(MemoryType type, String content, Double importance) {
        this.type = type;
        this.content = content;
        this.importance = importance;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public MemoryType getType() { return type; }
    public void setType(MemoryType type) { this.type = type; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Double getImportance() { return importance; }
    public void setImportance(Double importance) { this.importance = importance; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
