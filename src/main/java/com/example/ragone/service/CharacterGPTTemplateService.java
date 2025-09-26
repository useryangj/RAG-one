package com.example.ragone.service;

import com.example.ragone.dto.CharacterGPTConfig;
import com.example.ragone.entity.Character;
import com.example.ragone.entity.CharacterProfile;
import com.example.ragone.entity.DocumentChunk;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * CharacterGPT模板生成服务
 * 基于CharacterGPT提示词模板生成结构化的角色扮演提示词
 */
@Service
public class CharacterGPTTemplateService {
    
    private static final Logger logger = LoggerFactory.getLogger(CharacterGPTTemplateService.class);
    
    @Autowired
    private ChatLanguageModel chatLanguageModel;
    
    
    /**
     * 生成CharacterGPT格式的角色扮演提示词
     */
    public String generateCharacterGPTPrompt(Character character, CharacterProfile profile, List<DocumentChunk> chunks) {
        return generateCharacterGPTPrompt(character, profile, chunks, CharacterGPTConfig.getStandardConfig());
    }
    
    /**
     * 生成CharacterGPT格式的角色扮演提示词（带配置）
     */
    public String generateCharacterGPTPrompt(Character character, CharacterProfile profile, List<DocumentChunk> chunks, CharacterGPTConfig config) {
        logger.info("Generating CharacterGPT template for character: {} with config: {}", character.getId(), config.getTemplateType());
        
        if (!config.isEnabled()) {
            logger.info("CharacterGPT template generation is disabled for character: {}", character.getId());
            return generateFallbackTemplate(character, profile);
        }
        
        try {
            // 构建基础上下文
            String baseContext = buildBaseContext(character, chunks);
            
            // 使用AI生成各个部分
            CharacterGPTTemplate template = new CharacterGPTTemplate();
            
            if (config.isIncludeBasicInfo()) {
                template.setBasicInfo(generateBasicInfo(character, baseContext, chunks));
            }
            
            if (config.isIncludePersonalityTraits()) {
                template.setPersonalityTraits(generatePersonalityTraits(character, profile, baseContext, chunks));
            }
            
            if (config.isIncludeWorkflow()) {
                template.setWorkflow(generateWorkflow(character, profile, baseContext, chunks));
            }
            
            if (config.isIncludeSpeakingStyle()) {
                template.setSpeakingStyle(generateSpeakingStyle(character, profile, baseContext, chunks));
            }
            
            if (config.isIncludeBackgroundSetting()) {
                template.setBackgroundSetting(generateBackgroundSetting(character, profile, baseContext, chunks));
            }
            
            if (config.isIncludeInteractionRules()) {
                template.setInteractionRules(generateInteractionRules(character, profile, baseContext, chunks));
            }
            
            if (config.isIncludeExamples()) {
                template.setExamples(generateExamples(character, profile, baseContext, chunks, config.getExampleCount()));
            }
            
            // 生成完整的CharacterGPT提示词
            String prompt = formatCharacterGPTPrompt(template, config);
            
            // 添加自定义前缀和后缀
            if (config.getCustomPrefix() != null && !config.getCustomPrefix().isEmpty()) {
                prompt = config.getCustomPrefix() + "\n\n" + prompt;
            }
            
            if (config.getCustomSuffix() != null && !config.getCustomSuffix().isEmpty()) {
                prompt = prompt + "\n\n" + config.getCustomSuffix();
            }
            
            return prompt;
            
        } catch (Exception e) {
            logger.error("Failed to generate CharacterGPT template for character: {}", character.getId(), e);
            return generateFallbackTemplate(character, profile);
        }
    }
    
    /**
     * 生成基本信息部分
     */
    private String generateBasicInfo(Character character, String baseContext, List<DocumentChunk> chunks) {
        String prompt = String.format("""
            基于以下信息生成角色的基本信息：
            
            角色名称：%s
            角色描述：%s
            
            相关知识库内容：
            %s
            
            请按照以下格式生成基本信息：
            - 姓名: {正式名}
            - 小名：{昵称}
            - 性别: {性别}
            - 年龄: {年龄}
            - 职业: {职业}
            - 家乡: {家乡}
            - 现居: {现居地}
            - 教育背景: {教育背景}
            
            请根据知识库内容合理推断并填写这些信息，如果某些信息无法确定，请用"未知"或"待定"表示。
            """, character.getName(), character.getDescription(),
            chunks.stream().map(DocumentChunk::getContent).limit(5).collect(Collectors.joining("\n")));
        
        try {
            return chatLanguageModel.generate(prompt);
        } catch (Exception e) {
            logger.warn("Failed to generate basic info with AI", e);
            return String.format("""
                - 姓名: %s
                - 小名：%s
                - 性别: 未知
                - 年龄: 未知
                - 职业: 未知
                - 家乡: 未知
                - 现居: 未知
                - 教育背景: 未知
                """, character.getName(), character.getName());
        }
    }
    
    /**
     * 生成性格特点部分
     */
    private String generatePersonalityTraits(Character character, CharacterProfile profile, String baseContext, List<DocumentChunk> chunks) {
        String prompt = String.format("""
            基于以下信息分析角色的性格特点：
            
            角色名称：%s
            角色描述：%s
            现有性格特征：%s
            
            相关知识库内容：
            %s
            
            请分析并总结角色的主要性格特点，包括：
            1. 核心性格特点（3-5个）
            2. 行为习惯和偏好
            3. 情感表达方式
            4. 社交风格
            5. 独特表达方式
            
            请以列表形式输出，每个特点用一句话描述，以"-"开头。
            """, character.getName(), character.getDescription(),
            profile.getPersonalityTraits() != null ? profile.getPersonalityTraits() : "无",
            chunks.stream().map(DocumentChunk::getContent).limit(6).collect(Collectors.joining("\n")));
        
        try {
            return chatLanguageModel.generate(prompt);
        } catch (Exception e) {
            logger.warn("Failed to generate personality traits with AI", e);
            return profile.getPersonalityTraits() != null ? profile.getPersonalityTraits() : 
                "- 友善温和\n- 智慧理性\n- 耐心细致\n- 幽默风趣";
        }
    }
    
    /**
     * 生成工作流程部分
     */
    private String generateWorkflow(Character character, CharacterProfile profile, String baseContext, List<DocumentChunk> chunks) {
        String prompt = String.format("""
            基于以下信息为角色设计互动工作流程：
            
            角色名称：%s
            角色描述：%s
            说话风格：%s
            
            相关知识库内容：
            %s
            
            请设计角色的互动工作流程，包括：
            1. 如何判断对话者的身份和关系
            2. 对不同关系的人采取不同的回复策略
            3. 情绪识别和回应方式
            4. 话题引导和转换
            5. 特殊情况处理
            
            请以列表形式输出，每个流程以"-"开头。
            """, character.getName(), character.getDescription(),
            profile.getSpeakingStyle() != null ? profile.getSpeakingStyle() : "温和友好",
            chunks.stream().map(DocumentChunk::getContent).limit(6).collect(Collectors.joining("\n")));
        
        try {
            return chatLanguageModel.generate(prompt);
        } catch (Exception e) {
            logger.warn("Failed to generate workflow with AI", e);
            return """
                - 根据对方回复的热情程度判断是否熟人
                - 对熟人，表现更加随意和亲密
                - 对非熟人，保持礼貌和正式
                - 根据话题内容调整回应风格
                - 遇到不确定的事情会询问或表达困惑
                """;
        }
    }
    
    /**
     * 生成说话风格部分
     */
    private String generateSpeakingStyle(Character character, CharacterProfile profile, String baseContext, List<DocumentChunk> chunks) {
        String prompt = String.format("""
            基于以下信息分析角色的说话风格：
            
            角色名称：%s
            角色描述：%s
            现有说话风格：%s
            
            相关知识库内容：
            %s
            
            请分析角色的说话风格，包括：
            1. 语言特点（正式/非正式、简洁/详细等）
            2. 常用表达方式和词汇
            3. 语气词和习惯用语
            4. 情感色彩
            5. 专业术语使用习惯
            
            请以列表形式输出，每个风格以"-"开头。
            """, character.getName(), character.getDescription(),
            profile.getSpeakingStyle() != null ? profile.getSpeakingStyle() : "温和友好",
            chunks.stream().map(DocumentChunk::getContent).limit(6).collect(Collectors.joining("\n")));
        
        try {
            return chatLanguageModel.generate(prompt);
        } catch (Exception e) {
            logger.warn("Failed to generate speaking style with AI", e);
            return profile.getSpeakingStyle() != null ? profile.getSpeakingStyle() : 
                "- 语言温和而富有智慧\n- 喜欢用比喻和故事来解释复杂概念\n- 善于倾听和引导对话";
        }
    }
    
    /**
     * 生成背景设定部分
     */
    private String generateBackgroundSetting(Character character, CharacterProfile profile, String baseContext, List<DocumentChunk> chunks) {
        String prompt = String.format("""
            基于以下信息为角色生成背景设定：
            
            角色名称：%s
            角色描述：%s
            背景故事：%s
            
            相关知识库内容：
            %s
            
            请生成角色的背景设定，包括：
            1. 家庭背景和成长经历
            2. 重要的人生事件
            3. 人际关系和社交背景
            4. 专业技能和知识来源
            5. 个人价值观和信念
            6. 兴趣爱好和生活方式
            
            请以列表形式输出，每个设定以"-"开头。
            """, character.getName(), character.getDescription(),
            profile.getBackgroundStory() != null ? profile.getBackgroundStory() : "无",
            chunks.stream().map(DocumentChunk::getContent).limit(8).collect(Collectors.joining("\n")));
        
        try {
            return chatLanguageModel.generate(prompt);
        } catch (Exception e) {
            logger.warn("Failed to generate background setting with AI", e);
            return profile.getBackgroundStory() != null ? profile.getBackgroundStory() : 
                "- 基于知识库内容构建的背景故事\n- 具有丰富的知识和经验\n- 善于帮助他人解决问题";
        }
    }
    
    /**
     * 生成互动规则部分
     */
    private String generateInteractionRules(Character character, CharacterProfile profile, String baseContext, List<DocumentChunk> chunks) {
        String prompt = String.format("""
            基于以下信息为角色设定互动规则：
            
            角色名称：%s
            角色描述：%s
            现有限制：%s
            目标动机：%s
            
            相关知识库内容：
            %s
            
            请为角色设定互动规则，包括：
            1. 情绪表达方式和边界
            2. 回复长度和风格变化规则
            3. 特殊情况处理方式
            4. 互动语气和态度
            5. 行为边界和禁忌
            6. 安全准则
            
            请以列表形式输出，每个规则以"-"开头。
            """, character.getName(), character.getDescription(),
            profile.getRestrictions() != null ? profile.getRestrictions() : "无",
            profile.getGoalsAndMotivations() != null ? profile.getGoalsAndMotivations() : "无",
            chunks.stream().map(DocumentChunk::getContent).limit(6).collect(Collectors.joining("\n")));
        
        try {
            return chatLanguageModel.generate(prompt);
        } catch (Exception e) {
            logger.warn("Failed to generate interaction rules with AI", e);
            return """
                - 保持角色一致性，不偏离设定
                - 根据对话内容调整回复长度和风格
                - 遇到敏感话题时礼貌地引导到其他话题
                - 不提供有害信息，不参与不当讨论
                - 保持友善和专业的态度
                """;
        }
    }
    
    /**
     * 生成对话示例部分
     */
    private String generateExamples(Character character, CharacterProfile profile, String baseContext, List<DocumentChunk> chunks) {
        return generateExamples(character, profile, baseContext, chunks, 3);
    }
    
    /**
     * 生成对话示例部分（带数量配置）
     */
    private String generateExamples(Character character, CharacterProfile profile, String baseContext, List<DocumentChunk> chunks, int exampleCount) {
        String prompt = String.format("""
            基于以下信息为角色生成对话示例：
            
            角色名称：%s
            角色描述：%s
            说话风格：%s
            
            相关知识库内容：
            %s
            
            请生成%d个对话示例，展示角色的说话风格和特点，包括：
            1. 问候语示例
            2. 回答问题的方式
            3. 表达观点的方式
            4. 告别语示例
            
            请以以下格式输出：
            Q：{示例问题}
            A：{示例回答}
            
            每个示例之间用空行分隔。
            """, character.getName(), character.getDescription(),
            profile.getSpeakingStyle() != null ? profile.getSpeakingStyle() : "温和友好",
            chunks.stream().map(DocumentChunk::getContent).limit(6).collect(Collectors.joining("\n")),
            exampleCount);
        
        try {
            return chatLanguageModel.generate(prompt);
        } catch (Exception e) {
            logger.warn("Failed to generate examples with AI", e);
            return String.format("""
                Q：你好，很高兴认识你！
                A：你好！我是%s，很高兴认识你！希望我们的对话能给你带来帮助。
                
                Q：你能帮我解决这个问题吗？
                A：当然可以！让我想想...基于我的理解，这个问题可以从几个角度来分析。
                
                Q：谢谢你的帮助！
                A：不客气！很高兴能帮到你，期待下次交流！
                """, character.getName());
        }
    }
    
    /**
     * 构建基础上下文
     */
    private String buildBaseContext(Character character, List<DocumentChunk> chunks) {
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("角色名称: ").append(character.getName()).append("\n");
        contextBuilder.append("角色描述: ").append(character.getDescription()).append("\n\n");
        contextBuilder.append("相关知识库内容:\n");
        
        for (DocumentChunk chunk : chunks) {
            contextBuilder.append("- ").append(chunk.getContent()).append("\n");
        }
        
        return contextBuilder.toString();
    }
    
    /**
     * 格式化CharacterGPT提示词
     */
    private String formatCharacterGPTPrompt(CharacterGPTTemplate template) {
        return formatCharacterGPTPrompt(template, CharacterGPTConfig.getStandardConfig());
    }
    
    /**
     * 格式化CharacterGPT提示词（带配置）
     */
    private String formatCharacterGPTPrompt(CharacterGPTTemplate template, CharacterGPTConfig config) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("# Role: 角色扮演\n\n");
        
        if (template.getBasicInfo() != null && config.isIncludeBasicInfo()) {
            prompt.append("## 基本信息\n");
            prompt.append(template.getBasicInfo()).append("\n\n");
        }
        
        if (template.getPersonalityTraits() != null && config.isIncludePersonalityTraits()) {
            prompt.append("## 性格特点\n");
            prompt.append(template.getPersonalityTraits()).append("\n\n");
        }
        
        if (template.getWorkflow() != null && config.isIncludeWorkflow()) {
            prompt.append("## workflow\n");
            prompt.append(template.getWorkflow()).append("\n\n");
        }
        
        if (template.getSpeakingStyle() != null && config.isIncludeSpeakingStyle()) {
            prompt.append("## 说话风格\n");
            prompt.append(template.getSpeakingStyle()).append("\n\n");
        }
        
        if (template.getBackgroundSetting() != null && config.isIncludeBackgroundSetting()) {
            prompt.append("## 背景设定\n");
            prompt.append(template.getBackgroundSetting()).append("\n\n");
        }
        
        if (template.getInteractionRules() != null && config.isIncludeInteractionRules()) {
            prompt.append("## 互动规则\n");
            prompt.append(template.getInteractionRules()).append("\n\n");
        }
        
        if (template.getExamples() != null && config.isIncludeExamples()) {
            prompt.append("## Example\n");
            prompt.append(template.getExamples());
        }
        
        return prompt.toString();
    }
    
    /**
     * 生成降级模板
     */
    private String generateFallbackTemplate(Character character, CharacterProfile profile) {
        return String.format("""
            # Role: 角色扮演
            
            ## 基本信息
            - 姓名: %s
            - 小名：%s
            - 性别: 未知
            - 年龄: 未知
            - 职业: 未知
            - 家乡: 未知
            - 现居: 未知
            - 教育背景: 未知
            
            ## 性格特点
            %s
            
            ## workflow
            - 根据对方回复的热情程度判断是否熟人
            - 对熟人，表现更加随意和亲密
            - 对非熟人，保持礼貌和正式
            
            ## 说话风格
            %s
            
            ## 背景设定
            %s
            
            ## 互动规则
            - 保持角色一致性
            - 根据对话内容调整回复风格
            - 不提供有害信息
            
            ## Example
            Q：你好！
            A：你好！我是%s，很高兴认识你！
            """, 
            character.getName(),
            character.getName(),
            profile.getPersonalityTraits() != null ? profile.getPersonalityTraits() : "- 友善温和\n- 智慧理性",
            profile.getSpeakingStyle() != null ? profile.getSpeakingStyle() : "- 温和而富有智慧",
            profile.getBackgroundStory() != null ? profile.getBackgroundStory() : "- 基于知识库内容构建的背景",
            character.getName()
        );
    }
    
    /**
     * CharacterGPT模板数据结构
     */
    public static class CharacterGPTTemplate {
        private String basicInfo;
        private String personalityTraits;
        private String workflow;
        private String speakingStyle;
        private String backgroundSetting;
        private String interactionRules;
        private String examples;
        
        // Getters and Setters
        public String getBasicInfo() { return basicInfo; }
        public void setBasicInfo(String basicInfo) { this.basicInfo = basicInfo; }
        
        public String getPersonalityTraits() { return personalityTraits; }
        public void setPersonalityTraits(String personalityTraits) { this.personalityTraits = personalityTraits; }
        
        public String getWorkflow() { return workflow; }
        public void setWorkflow(String workflow) { this.workflow = workflow; }
        
        public String getSpeakingStyle() { return speakingStyle; }
        public void setSpeakingStyle(String speakingStyle) { this.speakingStyle = speakingStyle; }
        
        public String getBackgroundSetting() { return backgroundSetting; }
        public void setBackgroundSetting(String backgroundSetting) { this.backgroundSetting = backgroundSetting; }
        
        public String getInteractionRules() { return interactionRules; }
        public void setInteractionRules(String interactionRules) { this.interactionRules = interactionRules; }
        
        public String getExamples() { return examples; }
        public void setExamples(String examples) { this.examples = examples; }
    }
}
