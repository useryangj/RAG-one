package com.example.ragone.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 角色扮演会话实体类
 */
@Entity
@Table(name = "roleplay_sessions", indexes = {
    @Index(name = "idx_rp_session_user_id", columnList = "user_id"),
    @Index(name = "idx_rp_session_character_id", columnList = "character_id"),
    @Index(name = "idx_rp_session_id", columnList = "session_id")
})
public class RolePlaySession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore  // 前端不需要数据库主键ID
    private Long id;
    
    @Column(name = "session_id", nullable = false, unique = true)
    @JsonProperty("sessionId")  // 使用sessionId作为JSON字段名
    private String sessionId;
    
    @Column(name = "session_name")
    @JsonProperty("name")  // 前端期望的字段名
    private String sessionName;
    
    // 会话所属用户
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;
    
    // 会话关联的角色
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    @JsonIgnore  // 避免序列化懒加载对象
    private Character character;
    
    // 角色ID（用于前端序列化）
    @Column(name = "character_id", insertable = false, updatable = false)
    private Long characterId;
    
    // 会话状态
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status = SessionStatus.ACTIVE;
    
    // 会话配置（如温度、最大token等）
    @Column(name = "session_config", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String sessionConfig;
    
    // 会话统计信息
    @Column(name = "message_count", nullable = false)
    private Integer messageCount = 0;
    
    @Column(name = "total_tokens")
    @JsonProperty("tokenUsage")  // 前端期望的字段名
    private Long totalTokens = 0L;
    
    @Column(name = "last_activity_at")
    @JsonProperty("lastActiveAt")  // 前端期望的字段名
    private LocalDateTime lastActivityAt;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // 一个会话包含多条对话历史
    @OneToMany(mappedBy = "rolePlaySession", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<RolePlayHistory> rolePlayHistories;
    
    public enum SessionStatus {
        ACTIVE,     // 活跃状态
        PAUSED,     // 暂停状态
        ENDED,      // 已结束
        ARCHIVED,   // 已归档
        DELETED     // 已删除
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        lastActivityAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        lastActivityAt = LocalDateTime.now();
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
    
    public String getSessionName() {
        return sessionName;
    }
    
    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
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
    
    public Long getCharacterId() {
        return characterId;
    }
    
    public void setCharacterId(Long characterId) {
        this.characterId = characterId;
    }
    
    public SessionStatus getStatus() {
        return status;
    }
    
    public void setStatus(SessionStatus status) {
        this.status = status;
    }
    
    public String getSessionConfig() {
        return sessionConfig;
    }
    
    public void setSessionConfig(String sessionConfig) {
        this.sessionConfig = sessionConfig;
    }
    
    public Integer getMessageCount() {
        return messageCount;
    }
    
    public void setMessageCount(Integer messageCount) {
        this.messageCount = messageCount;
    }
    
    public Long getTotalTokens() {
        return totalTokens;
    }
    
    public void setTotalTokens(Long totalTokens) {
        this.totalTokens = totalTokens;
    }
    
    public LocalDateTime getLastActivityAt() {
        return lastActivityAt;
    }
    
    public void setLastActivityAt(LocalDateTime lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Set<RolePlayHistory> getRolePlayHistories() {
        return rolePlayHistories;
    }
    
    public void setRolePlayHistories(Set<RolePlayHistory> rolePlayHistories) {
        this.rolePlayHistories = rolePlayHistories;
    }
}