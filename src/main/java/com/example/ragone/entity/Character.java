package com.example.ragone.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 角色实体类 - 用于角色扮演系统
 */
@Entity
@Table(name = "characters", indexes = {
    @Index(name = "idx_character_user_id", columnList = "user_id"),
    @Index(name = "idx_character_name", columnList = "name"),
    @Index(name = "idx_character_kb_id", columnList = "knowledge_base_id")
})
public class Character {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "角色名称不能为空")
    @Size(min = 1, max = 100, message = "角色名称长度必须在1-100个字符之间")
    @Column(nullable = false)
    private String name;
    
    @Column(length = 500)
    private String description;
    
    @Column(name = "avatar_url")
    private String avatarUrl;
    
    // 角色所属用户
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;
    
    // 角色关联的知识库（用于生成人物卡和对话检索）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "knowledge_base_id", nullable = false)
    private KnowledgeBase knowledgeBase;
    
    // 角色状态
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CharacterStatus status = CharacterStatus.DRAFT;
    
    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = false;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // 一个角色有一个人物卡配置
    @OneToOne(mappedBy = "character", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private CharacterProfile profile;
    
    // 一个角色可以有多个对话会话
    @OneToMany(mappedBy = "character", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<RolePlaySession> rolePlaySessions;
    
    public enum CharacterStatus {
        DRAFT,      // 草稿状态
        ACTIVE,     // 激活状态
        INACTIVE,   // 非激活状态
        GENERATING  // 人物卡生成中
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getAvatarUrl() {
        return avatarUrl;
    }
    
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
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
    
    public CharacterStatus getStatus() {
        return status;
    }
    
    public void setStatus(CharacterStatus status) {
        this.status = status;
    }
    
    public Boolean getIsPublic() {
        return isPublic;
    }
    
    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
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
    
    public CharacterProfile getProfile() {
        return profile;
    }
    
    public void setProfile(CharacterProfile profile) {
        this.profile = profile;
    }
    
    public Set<RolePlaySession> getRolePlaySessions() {
        return rolePlaySessions;
    }
    
    public void setRolePlaySessions(Set<RolePlaySession> rolePlaySessions) {
        this.rolePlaySessions = rolePlaySessions;
    }
}