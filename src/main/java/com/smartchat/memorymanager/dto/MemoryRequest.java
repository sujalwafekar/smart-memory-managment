package com.smartchat.memorymanager.dto;

import com.smartchat.memorymanager.model.MemoryType;

public class MemoryRequest {
    private MemoryType type;
    private String content;
    private Double importance;

    public MemoryRequest() {}

    public MemoryRequest(MemoryType type, String content, Double importance) {
        this.type = type;
        this.content = content;
        this.importance = importance;
    }

    public MemoryType getType() { return type; }
    public void setType(MemoryType type) { this.type = type; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Double getImportance() { return importance; }
    public void setImportance(Double importance) { this.importance = importance; }
}
