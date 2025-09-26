package com.example.ragone.service;

import com.example.ragone.entity.Character;
import com.example.ragone.entity.CharacterProfile;
import com.example.ragone.entity.DocumentChunk;
import com.example.ragone.repository.CharacterProfileRepository;
import com.example.ragone.repository.DocumentChunkRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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
    
    @Autowired
    private ChatLanguageModel chatLanguageModel;
    
    @Autowired
    private CharacterGPTTemplateService characterGPTTemplateService;
    
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
        
        // 设置默认的system_prompt以避免null约束违反
        profile.setSystemPrompt("正在生成角色配置文件...");
        
        // 保存初始状态
        profile = characterProfileRepository.save(profile);
        
        try {
            // 从知识库检索相关内容
            List<DocumentChunk> relevantChunks = retrieveRelevantContent(character);
            
            // 生成配置文件内容
            generateProfileContent(profile, character, relevantChunks);
            
            // 生成CharacterGPT格式的系统提示词
            generateCharacterGPTSystemPrompt(profile, character, relevantChunks);
            
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
            
            // 确保在失败状态下也有有效的system_prompt
            if (profile.getSystemPrompt() == null || profile.getSystemPrompt().isEmpty()) {
                profile.setSystemPrompt("角色配置文件生成失败，请稍后重试。");
            }
            
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
     * 生成CharacterGPT格式的系统提示词
     */
    private void generateCharacterGPTSystemPrompt(CharacterProfile profile, Character character, List<DocumentChunk> chunks) {
        logger.debug("Generating CharacterGPT system prompt for character: {}", character.getId());
        
        try {
            // 使用CharacterGPT模板服务生成结构化提示词
            String characterGPTPrompt = characterGPTTemplateService.generateCharacterGPTPrompt(character, profile, chunks);
            
            // 将CharacterGPT格式的提示词作为系统提示词
            profile.setSystemPrompt(characterGPTPrompt);
            
            logger.info("CharacterGPT system prompt generated successfully for character: {}", character.getId());
            
        } catch (Exception e) {
            logger.error("Failed to generate CharacterGPT system prompt for character: {}", character.getId(), e);
            // 如果生成失败，使用原有的系统提示词生成方法
            profile.setSystemPrompt(generateSystemPromptWithAI(character, buildBaseContext(character, chunks), chunks));
        }
    }
    
    /**
     * 生成配置文件内容
     */
    private void generateProfileContent(CharacterProfile profile, Character character, List<DocumentChunk> chunks) {
        logger.debug("Generating profile content for character: {}", character.getId());
        
        try {
            // 构建基础上下文
            String baseContext = buildBaseContext(character, chunks);
            
            // 使用AI服务生成各个字段
            profile.setSystemPrompt(generateSystemPromptWithAI(character, baseContext, chunks));
            profile.setBackgroundStory(generateBackgroundStoryWithAI(character, baseContext, chunks));
            profile.setPersonalityTraits(generatePersonalityTraitsWithAI(character, baseContext, chunks));
            profile.setSpeakingStyle(generateSpeakingStyleWithAI(character, baseContext, chunks));
            profile.setInterests(generateInterestsWithAI(character, baseContext, chunks));
            profile.setExpertise(generateExpertiseWithAI(character, baseContext, chunks));
            profile.setEmotionalPatterns(generateEmotionalPatternsWithAI(character, baseContext, chunks));
            profile.setConversationExamples(generateConversationExamplesWithAI(character, baseContext, chunks));
            profile.setRestrictions(generateRestrictionsWithAI(character, baseContext, chunks));
            profile.setGoalsAndMotivations(generateGoalsAndMotivationsWithAI(character, baseContext, chunks));
            
            // 设置生成配置
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("chunksUsed", chunks.size());
            generationConfig.put("generatedAt", LocalDateTime.now().toString());
            generationConfig.put("model", "gpt-4");
            generationConfig.put("aiGenerated", true);
            
            profile.setGenerationConfig(objectMapper.writeValueAsString(generationConfig));
            
        } catch (Exception e) {
            logger.error("Failed to generate profile content with AI, falling back to basic generation", e);
            // 降级到基础生成
            generateProfileContentFallback(profile, character, chunks);
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
     * 降级生成方法（当AI服务不可用时）
     */
    private void generateProfileContentFallback(CharacterProfile profile, Character character, List<DocumentChunk> chunks) {
        String context = buildBaseContext(character, chunks);
        
        profile.setSystemPrompt(generateSystemPrompt(character, context));
        profile.setBackgroundStory("基于知识库内容生成的背景故事...");
        profile.setPersonalityTraits("友善、智慧、耐心、幽默");
        profile.setSpeakingStyle("温和而富有智慧，喜欢用比喻和故事来解释复杂概念");
        profile.setInterests("阅读、思考、帮助他人解决问题");
        profile.setExpertise("基于知识库内容的专业领域");
        profile.setEmotionalPatterns("情绪稳定，善于倾听，能够感同身受");
        profile.setConversationExamples(generateConversationExamples(character, context));
        profile.setRestrictions("不提供有害信息，不参与不当讨论，保持角色一致性");
        profile.setGoalsAndMotivations("帮助用户获得有价值的信息和见解，提供有意义的对话体验");
        
        // 设置生成配置
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("chunksUsed", chunks.size());
        generationConfig.put("generatedAt", LocalDateTime.now().toString());
        generationConfig.put("model", "fallback");
        generationConfig.put("aiGenerated", false);
        
        try {
            profile.setGenerationConfig(objectMapper.writeValueAsString(generationConfig));
        } catch (JsonProcessingException e) {
            logger.warn("Failed to serialize generation config", e);
        }
    }
    
    // ==================== AI驱动的生成方法 ====================
    
    /**
     * 使用AI生成系统提示词
     */
    private String generateSystemPromptWithAI(Character character, String baseContext, List<DocumentChunk> chunks) {
        String prompt = String.format("""
            基于以下信息为角色生成系统提示词：
            
            角色名称：%s
            角色描述：%s
            
            相关知识库内容：
            %s
            
            请生成一个专业的系统提示词，用于指导AI扮演这个角色。提示词应该：
            1. 明确角色的身份和背景
            2. 描述角色的性格特点和说话风格
            3. 设定角色的行为准则和限制
            4. 提供角色扮演的指导原则
            
            请直接输出系统提示词，不要包含其他解释。
            """, character.getName(), character.getDescription(), 
            chunks.stream().map(DocumentChunk::getContent).limit(5).collect(Collectors.joining("\n")));
        
        try {
            return chatLanguageModel.generate(prompt);
        } catch (Exception e) {
            logger.warn("Failed to generate system prompt with AI", e);
            return generateSystemPrompt(character, baseContext);
        }
    }
    
    /**
     * 使用AI生成背景故事
     */
    private String generateBackgroundStoryWithAI(Character character, String baseContext, List<DocumentChunk> chunks) {
        String prompt = String.format("""
            基于以下信息为角色生成详细的背景故事：
            
            角色名称：%s
            角色描述：%s
            
            相关知识库内容：
            %s
            
            请生成一个丰富、详细的背景故事，包括：
            1. 角色的成长经历
            2. 重要的人生事件
            3. 人际关系和社交背景
            4. 专业技能和知识来源
            5. 个人价值观和信念
            
            背景故事应该与知识库内容保持一致，字数控制在300-500字。
            """, character.getName(), character.getDescription(),
            chunks.stream().map(DocumentChunk::getContent).limit(8).collect(Collectors.joining("\n")));
        
        try {
            return chatLanguageModel.generate(prompt);
        } catch (Exception e) {
            logger.warn("Failed to generate background story with AI", e);
            return "基于知识库内容生成的背景故事...";
        }
    }
    
    /**
     * 使用AI生成性格特征
     */
    private String generatePersonalityTraitsWithAI(Character character, String baseContext, List<DocumentChunk> chunks) {
        String prompt = String.format("""
            基于以下信息分析角色的性格特征：
            
            角色名称：%s
            角色描述：%s
            
            相关知识库内容：
            %s
            
            请分析并总结角色的主要性格特征，包括：
            1. 核心性格特点（3-5个）
            2. 行为习惯和偏好
            3. 情感表达方式
            4. 社交风格
            
            请以简洁的列表形式输出，每个特征用一句话描述。
            """, character.getName(), character.getDescription(),
            chunks.stream().map(DocumentChunk::getContent).limit(6).collect(Collectors.joining("\n")));
        
        try {
            return chatLanguageModel.generate(prompt);
        } catch (Exception e) {
            logger.warn("Failed to generate personality traits with AI", e);
            return "友善、智慧、耐心、幽默";
        }
    }
    
    /**
     * 使用AI生成说话风格
     */
    private String generateSpeakingStyleWithAI(Character character, String baseContext, List<DocumentChunk> chunks) {
        String prompt = String.format("""
            基于以下信息分析角色的说话风格：
            
            角色名称：%s
            角色描述：%s
            
            相关知识库内容：
            %s
            
            请分析角色的说话风格，包括：
            1. 语言特点（正式/非正式、简洁/详细等）
            2. 常用表达方式
            3. 情感色彩
            4. 专业术语使用习惯
            
            请用2-3句话描述角色的说话风格。
            """, character.getName(), character.getDescription(),
            chunks.stream().map(DocumentChunk::getContent).limit(6).collect(Collectors.joining("\n")));
        
        try {
            return chatLanguageModel.generate(prompt);
        } catch (Exception e) {
            logger.warn("Failed to generate speaking style with AI", e);
            return "温和而富有智慧，喜欢用比喻和故事来解释复杂概念";
        }
    }
    
    /**
     * 使用AI生成兴趣爱好
     */
    private String generateInterestsWithAI(Character character, String baseContext, List<DocumentChunk> chunks) {
        String prompt = String.format("""
            基于以下信息分析角色的兴趣爱好：
            
            角色名称：%s
            角色描述：%s
            
            相关知识库内容：
            %s
            
            请分析角色的兴趣爱好，包括：
            1. 主要兴趣领域
            2. 业余爱好
            3. 学习偏好
            4. 娱乐方式
            
            请列出3-5个具体的兴趣爱好。
            """, character.getName(), character.getDescription(),
            chunks.stream().map(DocumentChunk::getContent).limit(6).collect(Collectors.joining("\n")));
        
        try {
            return chatLanguageModel.generate(prompt);
        } catch (Exception e) {
            logger.warn("Failed to generate interests with AI", e);
            return "阅读、思考、帮助他人解决问题";
        }
    }
    
    /**
     * 使用AI生成专业领域
     */
    private String generateExpertiseWithAI(Character character, String baseContext, List<DocumentChunk> chunks) {
        String prompt = String.format("""
            基于以下信息分析角色的专业领域：
            
            角色名称：%s
            角色描述：%s
            
            相关知识库内容：
            %s
            
            请分析角色的专业领域，包括：
            1. 核心专业技能
            2. 知识深度
            3. 实践经验
            4. 专业认证或资质
            
            请列出3-5个具体的专业领域。
            """, character.getName(), character.getDescription(),
            chunks.stream().map(DocumentChunk::getContent).limit(8).collect(Collectors.joining("\n")));
        
        try {
            return chatLanguageModel.generate(prompt);
        } catch (Exception e) {
            logger.warn("Failed to generate expertise with AI", e);
            return "基于知识库内容的专业领域";
        }
    }
    
    /**
     * 使用AI生成情感模式
     */
    private String generateEmotionalPatternsWithAI(Character character, String baseContext, List<DocumentChunk> chunks) {
        String prompt = String.format("""
            基于以下信息分析角色的情感模式：
            
            角色名称：%s
            角色描述：%s
            
            相关知识库内容：
            %s
            
            请分析角色的情感模式，包括：
            1. 情感表达方式
            2. 情绪调节能力
            3. 情感反应模式
            4. 情感稳定性
            
            请用2-3句话描述角色的情感模式。
            """, character.getName(), character.getDescription(),
            chunks.stream().map(DocumentChunk::getContent).limit(6).collect(Collectors.joining("\n")));
        
        try {
            return chatLanguageModel.generate(prompt);
        } catch (Exception e) {
            logger.warn("Failed to generate emotional patterns with AI", e);
            return "情绪稳定，善于倾听，能够感同身受";
        }
    }
    
    /**
     * 使用AI生成对话示例
     */
    private String generateConversationExamplesWithAI(Character character, String baseContext, List<DocumentChunk> chunks) {
        String prompt = String.format("""
            基于以下信息为角色生成对话示例：
            
            角色名称：%s
            角色描述：%s
            
            相关知识库内容：
            %s
            
            请生成3-5个对话示例，展示角色的说话风格和特点，包括：
            1. 问候语
            2. 回答问题的方式
            3. 表达观点的方式
            4. 告别语
            
            请以JSON格式输出，包含greeting、question_response、opinion_expression、farewell等字段。
            """, character.getName(), character.getDescription(),
            chunks.stream().map(DocumentChunk::getContent).limit(6).collect(Collectors.joining("\n")));
        
        try {
            return chatLanguageModel.generate(prompt);
        } catch (Exception e) {
            logger.warn("Failed to generate conversation examples with AI", e);
            return generateConversationExamples(character, baseContext);
        }
    }
    
    /**
     * 使用AI生成限制条件
     */
    private String generateRestrictionsWithAI(Character character, String baseContext, List<DocumentChunk> chunks) {
        String prompt = String.format("""
            基于以下信息为角色生成行为限制条件：
            
            角色名称：%s
            角色描述：%s
            
            相关知识库内容：
            %s
            
            请为角色设定适当的行为限制，包括：
            1. 不提供的内容类型
            2. 避免的话题
            3. 行为边界
            4. 安全准则
            
            请列出3-5条具体的限制条件。
            """, character.getName(), character.getDescription(),
            chunks.stream().map(DocumentChunk::getContent).limit(6).collect(Collectors.joining("\n")));
        
        try {
            return chatLanguageModel.generate(prompt);
        } catch (Exception e) {
            logger.warn("Failed to generate restrictions with AI", e);
            return "不提供有害信息，不参与不当讨论，保持角色一致性";
        }
    }
    
    /**
     * 使用AI生成目标动机
     */
    private String generateGoalsAndMotivationsWithAI(Character character, String baseContext, List<DocumentChunk> chunks) {
        String prompt = String.format("""
            基于以下信息分析角色的目标动机：
            
            角色名称：%s
            角色描述：%s
            
            相关知识库内容：
            %s
            
            请分析角色的目标动机，包括：
            1. 主要目标
            2. 内在动机
            3. 价值追求
            4. 人生使命
            
            请用2-3句话描述角色的目标动机。
            """, character.getName(), character.getDescription(),
            chunks.stream().map(DocumentChunk::getContent).limit(6).collect(Collectors.joining("\n")));
        
        try {
            return chatLanguageModel.generate(prompt);
        } catch (Exception e) {
            logger.warn("Failed to generate goals and motivations with AI", e);
            return "帮助用户获得有价值的信息和见解，提供有意义的对话体验";
        }
    }
    
    // ==================== 降级方法（原有实现） ====================
    
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
            
            // 确保有有效的system_prompt
            if (profile.getSystemPrompt() == null || profile.getSystemPrompt().isEmpty()) {
                profile.setSystemPrompt("角色配置文件生成失败：" + errorMessage);
            }
            
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
     * 重新生成CharacterGPT格式的系统提示词
     */
    public void regenerateCharacterGPTPrompt(Character character) {
        logger.info("Regenerating CharacterGPT prompt for character: {}", character.getId());
        
        try {
            Optional<CharacterProfile> profileOpt = characterProfileRepository.findByCharacter(character);
            if (profileOpt.isEmpty()) {
                logger.warn("Character profile not found for character: {}", character.getId());
                return;
            }
            
            CharacterProfile profile = profileOpt.get();
            List<DocumentChunk> relevantChunks = retrieveRelevantContent(character);
            
            // 生成新的CharacterGPT格式提示词
            generateCharacterGPTSystemPrompt(profile, character, relevantChunks);
            
            // 更新版本和时间戳
            profile.setVersion(profile.getVersion() + 1);
            profile.setUpdatedAt(LocalDateTime.now());
            
            characterProfileRepository.save(profile);
            logger.info("CharacterGPT prompt regenerated successfully for character: {}", character.getId());
            
        } catch (Exception e) {
            logger.error("Failed to regenerate CharacterGPT prompt for character: {}", character.getId(), e);
        }
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