package com.example.ragone.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 聊天会话DTO
 */
public class ChatSession {
    
    private String sessionId;
    private Long userId;
    private Long knowledgeBaseId;
    private String knowledgeBaseName;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastActiveAt;
    
    private List<ChatMessage> messages;
    private int messageCount;
    
    public ChatSession() {
        this.messages = new ArrayList<>();
        this.messageCount = 0;
    }
    
    public ChatSession(String sessionId, Long userId, Long knowledgeBaseId) {
        this();
        this.sessionId = sessionId;
        this.userId = userId;
        this.knowledgeBaseId = knowledgeBaseId;
        this.createdAt = LocalDateTime.now();
        this.lastActiveAt = LocalDateTime.now();
    }
    
    public void addMessage(ChatMessage message) {
        this.messages.add(message);
        this.messageCount++;
        this.lastActiveAt = LocalDateTime.now();
    }
    
    public void addUserMessage(String content) {
        addMessage(new ChatMessage("user", content));
    }
    
    public void addAssistantMessage(String content, String contextChunks) {
        addMessage(new ChatMessage("assistant", content, contextChunks));
    }
    
    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Long getKnowledgeBaseId() {
        return knowledgeBaseId;
    }
    
    public void setKnowledgeBaseId(Long knowledgeBaseId) {
        this.knowledgeBaseId = knowledgeBaseId;
    }
    
    public String getKnowledgeBaseName() {
        return knowledgeBaseName;
    }
    
    public void setKnowledgeBaseName(String knowledgeBaseName) {
        this.knowledgeBaseName = knowledgeBaseName;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getLastActiveAt() {
        return lastActiveAt;
    }
    
    public void setLastActiveAt(LocalDateTime lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }
    
    public List<ChatMessage> getMessages() {
        return messages;
    }
    
    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
        this.messageCount = messages != null ? messages.size() : 0;
    }
    
    public int getMessageCount() {
        return messageCount;
    }
    
    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }
}



