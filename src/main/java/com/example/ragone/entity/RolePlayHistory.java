package com.example.ragone.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 角色扮演对话历史实体类
 */
@Entity
@Table(name = "roleplay_histories", indexes = {
    @Index(name = "idx_rp_history_session_id", columnList = "session_id"),
    @Index(name = "idx_rp_history_user_id", columnList = "user_id"),
    @Index(name = "idx_rp_history_created_at", columnList = "created_at")
})
public class RolePlayHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 关联角色扮演会话
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    @JsonIgnore
    private RolePlaySession rolePlaySession;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    private Character character;
    
    // 用户输入的消息
    @Column(name = "user_message", columnDefinition = "TEXT", nullable = false)
    private String userMessage;
    
    // 角色回复的消息
    @Column(name = "character_response", columnDefinition = "TEXT", nullable = false)
    private String characterResponse;
    
    // 从知识库检索到的上下文片段
    @Column(name = "context_chunks", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String contextChunks;
    
    // 使用的系统提示词（可能会动态调整）
    @Column(name = "system_prompt_used", columnDefinition = "TEXT")
    private String systemPromptUsed;
    
    // 响应时间（毫秒）
    @Column(name = "response_time_ms")
    private Long responseTimeMs;
    
    // Token使用情况
    @Column(name = "token_usage", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String tokenUsage;
    
    // 情感分析结果（可选）
    @Column(name = "sentiment_analysis", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String sentimentAnalysis;
    
    // 用户反馈评分（1-5星）
    @Column(name = "user_rating")
    private Integer userRating;
    
    // 用户反馈内容
    @Column(name = "user_feedback", columnDefinition = "TEXT")
    private String userFeedback;
    
    // 对话轮次（在会话中的序号）
    @Column(name = "turn_number", nullable = false)
    private Integer turnNumber;
    
    // 是否使用了RAG检索
    @Column(name = "used_rag", nullable = false)
    private Boolean usedRag = true;
    
    // 检索到的相关文档数量
    @Column(name = "retrieved_chunks_count")
    private Integer retrievedChunksCount = 0;
    
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
    
    public RolePlaySession getRolePlaySession() {
        return rolePlaySession;
    }
    
    public void setRolePlaySession(RolePlaySession rolePlaySession) {
        this.rolePlaySession = rolePlaySession;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public Character getCharacter() {
        return character;
    }
    
    public void setCharacter(Character character) {
        this.character = character;
    }
    
    public String getUserMessage() {
        return userMessage;
    }
    
    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }
    
    public String getCharacterResponse() {
        return characterResponse;
    }
    
    public void setCharacterResponse(String characterResponse) {
        this.characterResponse = characterResponse;
    }
    
    public String getContextChunks() {
        return contextChunks;
    }
    
    public void setContextChunks(String contextChunks) {
        this.contextChunks = contextChunks;
    }
    
    public String getSystemPromptUsed() {
        return systemPromptUsed;
    }
    
    public void setSystemPromptUsed(String systemPromptUsed) {
        this.systemPromptUsed = systemPromptUsed;
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
    
    public String getSentimentAnalysis() {
        return sentimentAnalysis;
    }
    
    public void setSentimentAnalysis(String sentimentAnalysis) {
        this.sentimentAnalysis = sentimentAnalysis;
    }
    
    public Integer getUserRating() {
        return userRating;
    }
    
    public void setUserRating(Integer userRating) {
        this.userRating = userRating;
    }
    
    public String getUserFeedback() {
        return userFeedback;
    }
    
    public void setUserFeedback(String userFeedback) {
        this.userFeedback = userFeedback;
    }
    
    public Integer getTurnNumber() {
        return turnNumber;
    }
    
    public void setTurnNumber(Integer turnNumber) {
        this.turnNumber = turnNumber;
    }
    
    public Boolean getUsedRag() {
        return usedRag;
    }
    
    public void setUsedRag(Boolean usedRag) {
        this.usedRag = usedRag;
    }
    
    public Integer getRetrievedChunksCount() {
        return retrievedChunksCount;
    }
    
    public void setRetrievedChunksCount(Integer retrievedChunksCount) {
        this.retrievedChunksCount = retrievedChunksCount;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}