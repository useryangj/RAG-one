package com.example.ragone.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * CharacterGPT模板配置类
 * 用于配置CharacterGPT模板生成的各种参数
 */
public class CharacterGPTConfig {
    
    /**
     * 是否启用CharacterGPT模板
     */
    @JsonProperty("enabled")
    private boolean enabled = true;
    
    /**
     * 模板类型
     */
    @JsonProperty("template_type")
    private TemplateType templateType = TemplateType.STANDARD;
    
    /**
     * 是否包含基本信息部分
     */
    @JsonProperty("include_basic_info")
    private boolean includeBasicInfo = true;
    
    /**
     * 是否包含性格特点部分
     */
    @JsonProperty("include_personality_traits")
    private boolean includePersonalityTraits = true;
    
    /**
     * 是否包含工作流程部分
     */
    @JsonProperty("include_workflow")
    private boolean includeWorkflow = true;
    
    /**
     * 是否包含说话风格部分
     */
    @JsonProperty("include_speaking_style")
    private boolean includeSpeakingStyle = true;
    
    /**
     * 是否包含背景设定部分
     */
    @JsonProperty("include_background_setting")
    private boolean includeBackgroundSetting = true;
    
    /**
     * 是否包含互动规则部分
     */
    @JsonProperty("include_interaction_rules")
    private boolean includeInteractionRules = true;
    
    /**
     * 是否包含对话示例部分
     */
    @JsonProperty("include_examples")
    private boolean includeExamples = true;
    
    /**
     * 示例数量
     */
    @JsonProperty("example_count")
    private int exampleCount = 3;
    
    /**
     * 是否使用AI生成
     */
    @JsonProperty("use_ai_generation")
    private boolean useAIGeneration = true;
    
    /**
     * 自定义提示词前缀
     */
    @JsonProperty("custom_prefix")
    private String customPrefix = "";
    
    /**
     * 自定义提示词后缀
     */
    @JsonProperty("custom_suffix")
    private String customSuffix = "";
    
    /**
     * 模板类型枚举
     */
    public enum TemplateType {
        STANDARD,      // 标准模板
        MINIMAL,       // 精简模板
        DETAILED,      // 详细模板
        CUSTOM         // 自定义模板
    }
    
    // 构造函数
    public CharacterGPTConfig() {}
    
    public CharacterGPTConfig(boolean enabled) {
        this.enabled = enabled;
    }
    
    // Getters and Setters
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public TemplateType getTemplateType() {
        return templateType;
    }
    
    public void setTemplateType(TemplateType templateType) {
        this.templateType = templateType;
    }
    
    public boolean isIncludeBasicInfo() {
        return includeBasicInfo;
    }
    
    public void setIncludeBasicInfo(boolean includeBasicInfo) {
        this.includeBasicInfo = includeBasicInfo;
    }
    
    public boolean isIncludePersonalityTraits() {
        return includePersonalityTraits;
    }
    
    public void setIncludePersonalityTraits(boolean includePersonalityTraits) {
        this.includePersonalityTraits = includePersonalityTraits;
    }
    
    public boolean isIncludeWorkflow() {
        return includeWorkflow;
    }
    
    public void setIncludeWorkflow(boolean includeWorkflow) {
        this.includeWorkflow = includeWorkflow;
    }
    
    public boolean isIncludeSpeakingStyle() {
        return includeSpeakingStyle;
    }
    
    public void setIncludeSpeakingStyle(boolean includeSpeakingStyle) {
        this.includeSpeakingStyle = includeSpeakingStyle;
    }
    
    public boolean isIncludeBackgroundSetting() {
        return includeBackgroundSetting;
    }
    
    public void setIncludeBackgroundSetting(boolean includeBackgroundSetting) {
        this.includeBackgroundSetting = includeBackgroundSetting;
    }
    
    public boolean isIncludeInteractionRules() {
        return includeInteractionRules;
    }
    
    public void setIncludeInteractionRules(boolean includeInteractionRules) {
        this.includeInteractionRules = includeInteractionRules;
    }
    
    public boolean isIncludeExamples() {
        return includeExamples;
    }
    
    public void setIncludeExamples(boolean includeExamples) {
        this.includeExamples = includeExamples;
    }
    
    public int getExampleCount() {
        return exampleCount;
    }
    
    public void setExampleCount(int exampleCount) {
        this.exampleCount = exampleCount;
    }
    
    public boolean isUseAIGeneration() {
        return useAIGeneration;
    }
    
    public void setUseAIGeneration(boolean useAIGeneration) {
        this.useAIGeneration = useAIGeneration;
    }
    
    public String getCustomPrefix() {
        return customPrefix;
    }
    
    public void setCustomPrefix(String customPrefix) {
        this.customPrefix = customPrefix;
    }
    
    public String getCustomSuffix() {
        return customSuffix;
    }
    
    public void setCustomSuffix(String customSuffix) {
        this.customSuffix = customSuffix;
    }
    
    /**
     * 获取预设配置
     */
    public static CharacterGPTConfig getStandardConfig() {
        CharacterGPTConfig config = new CharacterGPTConfig();
        config.setTemplateType(TemplateType.STANDARD);
        return config;
    }
    
    public static CharacterGPTConfig getMinimalConfig() {
        CharacterGPTConfig config = new CharacterGPTConfig();
        config.setTemplateType(TemplateType.MINIMAL);
        config.setIncludeBackgroundSetting(false);
        config.setIncludeWorkflow(false);
        config.setExampleCount(2);
        return config;
    }
    
    public static CharacterGPTConfig getDetailedConfig() {
        CharacterGPTConfig config = new CharacterGPTConfig();
        config.setTemplateType(TemplateType.DETAILED);
        config.setExampleCount(5);
        return config;
    }
}
