package com.example.ragone.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 聊天消息DTO
 */
public class ChatMessage {
    
    private String role; // "user" 或 "assistant"
    private String content;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    private String contextChunks; // 关联的文档片段（JSON格式）
    
    public ChatMessage() {}
    
    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }
    
    public ChatMessage(String role, String content, String contextChunks) {
        this.role = role;
        this.content = content;
        this.contextChunks = contextChunks;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getContextChunks() {
        return contextChunks;
    }
    
    public void setContextChunks(String contextChunks) {
        this.contextChunks = contextChunks;
    }
}



