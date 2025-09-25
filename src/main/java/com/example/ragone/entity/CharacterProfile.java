package com.example.ragone.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 角色人物卡实体类 - 存储角色的详细配置信息
 */
@Entity
@Table(name = "character_profiles", indexes = {
    @Index(name = "idx_profile_character_id", columnList = "character_id")
})
public class CharacterProfile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 一对一关联角色
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    @JsonIgnore
    private Character character;
    
    // 系统提示词 - 角色的核心人设
    @Column(name = "system_prompt", columnDefinition = "TEXT", nullable = false)
    private String systemPrompt;
    
    // 角色背景故事
    @Column(name = "background_story", columnDefinition = "TEXT")
    private String backgroundStory;
    
    // 角色性格特征
    @Column(name = "personality_traits", columnDefinition = "TEXT")
    private String personalityTraits;
    
    // 说话风格和语言习惯
    @Column(name = "speaking_style", columnDefinition = "TEXT")
    private String speakingStyle;
    
    // 角色的兴趣爱好
    @Column(name = "interests", columnDefinition = "TEXT")
    private String interests;
    
    // 角色的专业技能或知识领域
    @Column(name = "expertise", columnDefinition = "TEXT")
    private String expertise;
    
    // 角色的情感状态和行为模式
    @Column(name = "emotional_patterns", columnDefinition = "TEXT")
    private String emotionalPatterns;
    
    // 对话示例 - JSON格式存储
    @Column(name = "conversation_examples", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String conversationExamples;
    
    // 角色的禁忌话题或行为限制
    @Column(name = "restrictions", columnDefinition = "TEXT")
    private String restrictions;
    
    // 角色的目标和动机
    @Column(name = "goals_and_motivations", columnDefinition = "TEXT")
    private String goalsAndMotivations;
    
    // 人物卡生成状态
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProfileStatus status = ProfileStatus.DRAFT;
    
    // 人物卡生成方式
    @Enumerated(EnumType.STRING)
    @Column(name = "generation_method", nullable = false)
    private GenerationMethod generationMethod = GenerationMethod.AI_GENERATED;
    
    // 生成时使用的模板或配置
    @Column(name = "generation_config", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String generationConfig;
    
    // 人物卡版本号
    @Column(name = "version", nullable = false)
    private Integer version = 1;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum ProfileStatus {
        DRAFT,      // 草稿状态
        GENERATING, // 生成中
        COMPLETED,  // 已完成
        FAILED      // 生成失败
    }
    
    public enum GenerationMethod {
        AI_GENERATED,   // AI自动生成
        MANUAL_CREATED, // 手动创建
        TEMPLATE_BASED  // 基于模板
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        version++;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Character getCharacter() {
        return character;
    }
    
    public void setCharacter(Character character) {
        this.character = character;
    }
    
    public String getSystemPrompt() {
        return systemPrompt;
    }
    
    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }
    
    public String getBackgroundStory() {
        return backgroundStory;
    }
    
    public void setBackgroundStory(String backgroundStory) {
        this.backgroundStory = backgroundStory;
    }
    
    public String getPersonalityTraits() {
        return personalityTraits;
    }
    
    public void setPersonalityTraits(String personalityTraits) {
        this.personalityTraits = personalityTraits;
    }
    
    public String getSpeakingStyle() {
        return speakingStyle;
    }
    
    public void setSpeakingStyle(String speakingStyle) {
        this.speakingStyle = speakingStyle;
    }
    
    public String getInterests() {
        return interests;
    }
    
    public void setInterests(String interests) {
        this.interests = interests;
    }
    
    public String getExpertise() {
        return expertise;
    }
    
    public void setExpertise(String expertise) {
        this.expertise = expertise;
    }
    
    public String getEmotionalPatterns() {
        return emotionalPatterns;
    }
    
    public void setEmotionalPatterns(String emotionalPatterns) {
        this.emotionalPatterns = emotionalPatterns;
    }
    
    public String getConversationExamples() {
        return conversationExamples;
    }
    
    public void setConversationExamples(String conversationExamples) {
        this.conversationExamples = conversationExamples;
    }
    
    public String getRestrictions() {
        return restrictions;
    }
    
    public void setRestrictions(String restrictions) {
        this.restrictions = restrictions;
    }
    
    public String getGoalsAndMotivations() {
        return goalsAndMotivations;
    }
    
    public void setGoalsAndMotivations(String goalsAndMotivations) {
        this.goalsAndMotivations = goalsAndMotivations;
    }
    
    public ProfileStatus getStatus() {
        return status;
    }
    
    public void setStatus(ProfileStatus status) {
        this.status = status;
    }
    
    public GenerationMethod getGenerationMethod() {
        return generationMethod;
    }
    
    public void setGenerationMethod(GenerationMethod generationMethod) {
        this.generationMethod = generationMethod;
    }
    
    public String getGenerationConfig() {
        return generationConfig;
    }
    
    public void setGenerationConfig(String generationConfig) {
        this.generationConfig = generationConfig;
    }
    
    public Integer getVersion() {
        return version;
    }
    
    public void setVersion(Integer version) {
        this.version = version;
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