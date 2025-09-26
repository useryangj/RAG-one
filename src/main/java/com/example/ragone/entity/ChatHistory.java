package com.example.ragone.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.JdbcTypeCode;

/**
 * 聊天历史实体类
 */
@Entity
@Table(name = "chat_histories", indexes = {
    @Index(name = "idx_chat_user_id", columnList = "user_id"),
    @Index(name = "idx_chat_session_id", columnList = "session_id"),
    @Index(name = "idx_chat_created_at", columnList = "created_at")
})
public class ChatHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "session_id", nullable = false)
    private String sessionId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore  // 避免序列化懒加载对象
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "knowledge_base_id")
    @JsonIgnore  // 避免序列化懒加载对象
    private KnowledgeBase knowledgeBase;
    
    @Column(name = "user_message", columnDefinition = "TEXT", nullable = false)
    private String userMessage;
    
    @Column(name = "assistant_response", columnDefinition = "TEXT", nullable = false)
    private String assistantResponse;
    
    @Column(name = "context_chunks", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String contextChunks;
    
    @Column(name = "response_time_ms")
    private Long responseTimeMs;
    
    @Column(name = "token_usage", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String tokenUsage;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public KnowledgeBase getKnowledgeBase() {
        return knowledgeBase;
    }
    
    public void setKnowledgeBase(KnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }
    
    public String getUserMessage() {
        return userMessage;
    }
    
    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }
    
    public String getAssistantResponse() {
        return assistantResponse;
    }
    
    public void setAssistantResponse(String assistantResponse) {
        this.assistantResponse = assistantResponse;
    }
    
    public String getContextChunks() {
        return contextChunks;
    }
    
    public void setContextChunks(String contextChunks) {
        this.contextChunks = contextChunks;
    }
    
    public Long getResponseTimeMs() {
        return responseTimeMs;
    }
    
    public void setResponseTimeMs(Long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }
    
    public String getTokenUsage() {
        return tokenUsage;
    }
    
    public void setTokenUsage(String tokenUsage) {
        this.tokenUsage = tokenUsage;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
