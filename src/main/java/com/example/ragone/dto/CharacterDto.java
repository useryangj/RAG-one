package com.example.ragone.dto;

import com.example.ragone.entity.Character;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 角色数据传输对象
 */
public class CharacterDto {
    
    private Long id;
    private String name;
    private String description;
    private String avatarUrl;
    private Character.CharacterStatus status;
    private Boolean isPublic;
    private Long knowledgeBaseId;
    private String knowledgeBaseName;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    // 构造函数
    public CharacterDto() {}
    
    public CharacterDto(Character character) {
        this.id = character.getId();
        this.name = character.getName();
        this.description = character.getDescription();
        this.avatarUrl = character.getAvatarUrl();
        this.status = character.getStatus();
        this.isPublic = character.getIsPublic();
        this.createdAt = character.getCreatedAt();
        this.updatedAt = character.getUpdatedAt();
        
        // 安全地获取知识库信息
        if (character.getKnowledgeBase() != null) {
            this.knowledgeBaseId = character.getKnowledgeBase().getId();
            this.knowledgeBaseName = character.getKnowledgeBase().getName();
        }
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
    
    public Character.CharacterStatus getStatus() {
        return status;
    }
    
    public void setStatus(Character.CharacterStatus status) {
        this.status = status;
    }
    
    public Boolean getIsPublic() {
        return isPublic;
    }
    
    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
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
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
