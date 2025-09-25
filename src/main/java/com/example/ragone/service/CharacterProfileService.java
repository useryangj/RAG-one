package com.example.ragone.service;

import com.example.ragone.entity.Character;
import com.example.ragone.entity.CharacterProfile;
import com.example.ragone.entity.DocumentChunk;
import com.example.ragone.repository.CharacterProfileRepository;
import com.example.ragone.repository.DocumentChunkRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 角色配置文件服务类
 */
@Service
@Transactional
public class CharacterProfileService {
    
    private static final Logger logger = LoggerFactory.getLogger(CharacterProfileService.class);
    
    @Autowired
    private CharacterProfileRepository characterProfileRepository;
    
    @Autowired
    private DocumentChunkRepository documentChunkRepository;
    
    @Autowired
    private HybridRetrievalService hybridRetrievalService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // TODO: 注入AI服务（如OpenAI API客户端）
    // @Autowired
    // private OpenAIService openAIService;
    
    /**
     * 异步生成角色配置文件
     */
    @Async
    public CompletableFuture<CharacterProfile> generateProfileAsync(Character character) {
        logger.info("Starting async profile generation for character: {}", character.getId());
        
        try {
            CharacterProfile profile = generateProfile(character);
            return CompletableFuture.completedFuture(profile);
        } catch (Exception e) {
            logger.error("Failed to generate profile for character: {}", character.getId(), e);
            markProfileAsFailed(character, e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * 生成角色配置文件
     */
    public CharacterProfile generateProfile(Character character) {
        logger.info("Generating profile for character: {}", character.getId());
        
        // 检查是否已有配置文件
        Optional<CharacterProfile> existingProfile = characterProfileRepository.findByCharacter(character);
        CharacterProfile profile;
        
        if (existingProfile.isPresent()) {
            profile = existingProfile.get();
            profile.setVersion(profile.getVersion() + 1);
        } else {
            profile = new CharacterProfile();
            profile.setCharacter(character);
            profile.setVersion(1);
        }
        
        // 设置生成状态
        profile.setStatus(CharacterProfile.ProfileStatus.GENERATING);
        profile.setGenerationMethod(CharacterProfile.GenerationMethod.AI_GENERATED);
        profile.setUpdatedAt(LocalDateTime.now());
        
        // 保存初始状态
        profile = characterProfileRepository.save(profile);
        
        try {
            // 从知识库检索相关内容
            List<DocumentChunk> relevantChunks = retrieveRelevantContent(character);
            
            // 生成配置文件内容
            generateProfileContent(profile, character, relevantChunks);
            
            // 标记为完成
            profile.setStatus(CharacterProfile.ProfileStatus.COMPLETED);
            profile.setUpdatedAt(LocalDateTime.now());
            
            profile = characterProfileRepository.save(profile);
            logger.info("Profile generated successfully for character: {}", character.getId());
            
            return profile;
            
        } catch (Exception e) {
            logger.error("Failed to generate profile content for character: {}", character.getId(), e);
            profile.setStatus(CharacterProfile.ProfileStatus.FAILED);
            profile.setUpdatedAt(LocalDateTime.now());
            characterProfileRepository.save(profile);
            throw e;
        }
    }
    
    /**
     * 从知识库检索相关内容
     */
    private List<DocumentChunk> retrieveRelevantContent(Character character) {
        logger.debug("Retrieving relevant content for character: {}", character.getId());
        
        // 构建检索查询
        List<String> queries = Arrays.asList(
            "角色性格 人物特点 个性",
            "说话方式 语言风格 表达习惯",
            "背景故事 经历 历史",
            "兴趣爱好 专长 技能",
            "目标 动机 愿望",
            character.getName() + " " + character.getDescription()
        );
        
        Set<DocumentChunk> allChunks = new HashSet<>();
        
        for (String query : queries) {
            try {
                List<DocumentChunk> chunks = hybridRetrievalService.hybridSearch(
                    query, 
                    character.getKnowledgeBase().getId()
                );
                // 限制每个查询的结果数量为5个
                if (chunks.size() > 5) {
                    chunks = chunks.subList(0, 5);
                }
                allChunks.addAll(chunks);
            } catch (Exception e) {
                logger.warn("Failed to retrieve chunks for query: {}", query, e);
            }
        }
        
        logger.debug("Retrieved {} unique chunks for character: {}", allChunks.size(), character.getId());
        return new ArrayList<>(allChunks);
    }
    
    /**
     * 生成配置文件内容
     */
    private void generateProfileContent(CharacterProfile profile, Character character, List<DocumentChunk> chunks) {
        logger.debug("Generating profile content for character: {}", character.getId());
        
        // 构建上下文内容
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("角色名称: ").append(character.getName()).append("\n");
        contextBuilder.append("角色描述: ").append(character.getDescription()).append("\n\n");
        contextBuilder.append("相关知识库内容:\n");
        
        for (DocumentChunk chunk : chunks) {
            contextBuilder.append("- ").append(chunk.getContent()).append("\n");
        }
        
        String context = contextBuilder.toString();
        
        // TODO: 调用AI服务生成各个字段
        // 这里先使用模拟数据，实际应该调用AI API
        
        // 生成系统提示词
        profile.setSystemPrompt(generateSystemPrompt(character, context));
        
        // 生成背景故事
        profile.setBackgroundStory(generateBackgroundStory(character, context));
        
        // 生成性格特征
        profile.setPersonalityTraits(generatePersonalityTraits(character, context));
        
        // 生成说话风格
        profile.setSpeakingStyle(generateSpeakingStyle(character, context));
        
        // 生成兴趣爱好
        profile.setInterests(generateInterests(character, context));
        
        // 生成专业领域
        profile.setExpertise(generateExpertise(character, context));
        
        // 生成情感模式
        profile.setEmotionalPatterns(generateEmotionalPatterns(character, context));
        
        // 生成对话示例
        profile.setConversationExamples(generateConversationExamples(character, context));
        
        // 生成限制条件
        profile.setRestrictions(generateRestrictions(character, context));
        
        // 生成目标动机
        profile.setGoalsAndMotivations(generateGoalsAndMotivations(character, context));
        
        // 设置生成配置
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("chunksUsed", chunks.size());
        generationConfig.put("generatedAt", LocalDateTime.now().toString());
        generationConfig.put("model", "gpt-4"); // TODO: 使用实际模型名称
        
        try {
            profile.setGenerationConfig(objectMapper.writeValueAsString(generationConfig));
        } catch (JsonProcessingException e) {
            logger.warn("Failed to serialize generation config", e);
        }
    }
    
    // TODO: 以下方法应该调用实际的AI服务
    // 现在使用模拟实现
    
    private String generateSystemPrompt(Character character, String context) {
        return String.format(
            "你是%s，%s。请根据以下背景信息进行角色扮演：\n\n%s\n\n" +
            "请始终保持角色一致性，用符合角色特点的方式回应用户。",
            character.getName(),
            character.getDescription(),
            context.length() > 1000 ? context.substring(0, 1000) + "..." : context
        );
    }
    
    private String generateBackgroundStory(Character character, String context) {
        // TODO: 调用AI生成背景故事
        return "基于知识库内容生成的背景故事...";
    }
    
    private String generatePersonalityTraits(Character character, String context) {
        // TODO: 调用AI生成性格特征
        return "友善、智慧、耐心、幽默";
    }
    
    private String generateSpeakingStyle(Character character, String context) {
        // TODO: 调用AI生成说话风格
        return "温和而富有智慧，喜欢用比喻和故事来解释复杂概念";
    }
    
    private String generateInterests(Character character, String context) {
        // TODO: 调用AI生成兴趣爱好
        return "阅读、思考、帮助他人解决问题";
    }
    
    private String generateExpertise(Character character, String context) {
        // TODO: 调用AI生成专业领域
        return "基于知识库内容的专业领域";
    }
    
    private String generateEmotionalPatterns(Character character, String context) {
        // TODO: 调用AI生成情感模式
        return "情绪稳定，善于倾听，能够感同身受";
    }
    
    private String generateConversationExamples(Character character, String context) {
        // TODO: 调用AI生成对话示例
        Map<String, Object> examples = new HashMap<>();
        examples.put("greeting", "你好！我是" + character.getName() + "，很高兴认识你！");
        examples.put("question_response", "这是一个很好的问题，让我想想...");
        examples.put("farewell", "希望我们的对话对你有帮助，期待下次交流！");
        
        try {
            return objectMapper.writeValueAsString(examples);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to serialize conversation examples", e);
            return "{}";
        }
    }
    
    private String generateRestrictions(Character character, String context) {
        // TODO: 调用AI生成限制条件
        return "不提供有害信息，不参与不当讨论，保持角色一致性";
    }
    
    private String generateGoalsAndMotivations(Character character, String context) {
        // TODO: 调用AI生成目标动机
        return "帮助用户获得有价值的信息和见解，提供有意义的对话体验";
    }
    
    /**
     * 标记配置文件生成失败
     */
    private void markProfileAsFailed(Character character, String errorMessage) {
        Optional<CharacterProfile> profileOpt = characterProfileRepository.findByCharacter(character);
        if (profileOpt.isPresent()) {
            CharacterProfile profile = profileOpt.get();
            profile.setStatus(CharacterProfile.ProfileStatus.FAILED);
            profile.setUpdatedAt(LocalDateTime.now());
            characterProfileRepository.save(profile);
        }
    }
    
    /**
     * 重新生成配置文件
     */
    public void regenerateProfile(Character character) {
        logger.info("Regenerating profile for character: {}", character.getId());
        generateProfileAsync(character);
    }
    
    /**
     * 检查配置文件是否完成
     */
    @Transactional(readOnly = true)
    public boolean isProfileCompleted(Character character) {
        Optional<CharacterProfile> profileOpt = characterProfileRepository.findByCharacter(character);
        return profileOpt.isPresent() && 
               profileOpt.get().getStatus() == CharacterProfile.ProfileStatus.COMPLETED;
    }
    
    /**
     * 获取角色的配置文件
     */
    @Transactional(readOnly = true)
    public Optional<CharacterProfile> getCharacterProfile(Character character) {
        return characterProfileRepository.findByCharacter(character);
    }
    
    /**
     * 更新配置文件
     */
    public CharacterProfile updateProfile(Character character, CharacterProfile updatedProfile) {
        logger.info("Updating profile for character: {}", character.getId());
        
        Optional<CharacterProfile> existingProfileOpt = characterProfileRepository.findByCharacter(character);
        if (existingProfileOpt.isEmpty()) {
            throw new IllegalArgumentException("角色配置文件不存在");
        }
        
        CharacterProfile existingProfile = existingProfileOpt.get();
        
        // 更新字段
        if (updatedProfile.getSystemPrompt() != null) {
            existingProfile.setSystemPrompt(updatedProfile.getSystemPrompt());
        }
        if (updatedProfile.getBackgroundStory() != null) {
            existingProfile.setBackgroundStory(updatedProfile.getBackgroundStory());
        }
        if (updatedProfile.getPersonalityTraits() != null) {
            existingProfile.setPersonalityTraits(updatedProfile.getPersonalityTraits());
        }
        if (updatedProfile.getSpeakingStyle() != null) {
            existingProfile.setSpeakingStyle(updatedProfile.getSpeakingStyle());
        }
        if (updatedProfile.getInterests() != null) {
            existingProfile.setInterests(updatedProfile.getInterests());
        }
        if (updatedProfile.getExpertise() != null) {
            existingProfile.setExpertise(updatedProfile.getExpertise());
        }
        if (updatedProfile.getEmotionalPatterns() != null) {
            existingProfile.setEmotionalPatterns(updatedProfile.getEmotionalPatterns());
        }
        if (updatedProfile.getConversationExamples() != null) {
            existingProfile.setConversationExamples(updatedProfile.getConversationExamples());
        }
        if (updatedProfile.getRestrictions() != null) {
            existingProfile.setRestrictions(updatedProfile.getRestrictions());
        }
        if (updatedProfile.getGoalsAndMotivations() != null) {
            existingProfile.setGoalsAndMotivations(updatedProfile.getGoalsAndMotivations());
        }
        
        existingProfile.setGenerationMethod(CharacterProfile.GenerationMethod.MANUAL_CREATED);
        existingProfile.setVersion(existingProfile.getVersion() + 1);
        existingProfile.setUpdatedAt(LocalDateTime.now());
        
        CharacterProfile savedProfile = characterProfileRepository.save(existingProfile);
        logger.info("Profile updated successfully for character: {}", character.getId());
        
        return savedProfile;
    }
}