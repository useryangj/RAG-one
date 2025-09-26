package com.example.ragone.service;

import com.example.ragone.entity.*;
import com.example.ragone.entity.Character;
import com.example.ragone.repository.RolePlayHistoryRepository;
import com.example.ragone.repository.RolePlaySessionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 角色扮演对话服务类
 */
@Service
@Transactional
public class RolePlayService {
    
    private static final Logger logger = LoggerFactory.getLogger(RolePlayService.class);
    
    @Autowired
    private RolePlaySessionRepository sessionRepository;
    
    @Autowired
    private RolePlayHistoryRepository historyRepository;
    
    @Autowired
    private CharacterService characterService;
    
    @Autowired
    private CharacterProfileService characterProfileService;
    
    @Autowired
    private HybridRetrievalService hybridRetrievalService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private ChatLanguageModel chatLanguageModel;
    
    /**
     * 创建新的角色扮演会话
     */
    public RolePlaySession createSession(User user, Long characterId, String sessionName) {
        logger.info("Creating roleplay session for user: {}, character: {}", user.getUsername(), characterId);
        
        // 验证角色是否存在且可用
        Character character = characterService.getCharacterByIdAndUser(characterId, user);
        if (character.getStatus() != Character.CharacterStatus.ACTIVE) {
            throw new IllegalStateException("角色未激活，无法开始对话");
        }
        
        // 检查角色配置文件是否完成
        if (!characterProfileService.isProfileCompleted(character)) {
            throw new IllegalStateException("角色配置文件未完成，无法开始对话");
        }
        
        // 生成会话ID
        String sessionId = generateSessionId();
        
        // 创建会话
        RolePlaySession session = new RolePlaySession();
        session.setSessionId(sessionId);
        session.setSessionName(sessionName != null ? sessionName : "与" + character.getName() + "的对话");
        session.setUser(user);
        session.setCharacter(character);
        session.setStatus(RolePlaySession.SessionStatus.ACTIVE);
        session.setLastActivityAt(LocalDateTime.now());
        
        // 设置默认会话配置
        Map<String, Object> config = new HashMap<>();
        config.put("maxHistoryLength", 20);
        config.put("useRAG", true);
        config.put("temperature", 0.7);
        config.put("maxTokens", 1000);
        
        try {
            session.setSessionConfig(objectMapper.writeValueAsString(config));
        } catch (JsonProcessingException e) {
            logger.warn("Failed to serialize session config", e);
        }
        
        RolePlaySession savedSession = sessionRepository.save(session);
        logger.info("Roleplay session created: {}", savedSession.getSessionId());
        
        return savedSession;
    }
    
    /**
     * 发送消息并获取角色回复
     */
    public RolePlayHistory sendMessage(User user, String sessionId, String userMessage) {
        logger.info("Processing message for session: {}, user: {}", sessionId, user.getUsername());
        
        long startTime = System.currentTimeMillis();
        
        // 获取会话
        RolePlaySession session = getSessionByIdAndUser(sessionId, user);
        if (session.getStatus() != RolePlaySession.SessionStatus.ACTIVE) {
            throw new IllegalStateException("会话未激活，无法发送消息");
        }
        
        // 获取角色和配置文件
        Character character = session.getCharacter();
        CharacterProfile profile = characterProfileService.getCharacterProfile(character)
                .orElseThrow(() -> new IllegalStateException("角色配置文件不存在"));
        
        try {
            // 获取对话历史
            List<RolePlayHistory> recentHistory = getRecentHistory(session, 10);
            
            // 从知识库检索相关内容
            List<DocumentChunk> contextChunks = retrieveRelevantContext(userMessage, character, recentHistory);
            
            // 构建对话上下文
            String conversationContext = buildConversationContext(profile, recentHistory, contextChunks, userMessage);
            
            // 生成角色回复
            String characterResponse = generateCharacterResponse(conversationContext, profile);
            
            // 计算轮次号
            Integer turnNumber = getNextTurnNumber(session);
            
            // 保存对话历史
            RolePlayHistory history = new RolePlayHistory();
            history.setRolePlaySession(session);
            history.setUser(user);
            history.setCharacter(character);
            history.setUserMessage(userMessage);
            history.setCharacterResponse(characterResponse);
            history.setSystemPromptUsed(profile.getSystemPrompt());
            history.setTurnNumber(turnNumber);
            history.setUsedRag(!contextChunks.isEmpty());
            history.setRetrievedChunksCount(contextChunks.size());
            history.setResponseTimeMs(System.currentTimeMillis() - startTime);
            
            // 保存上下文片段信息
            if (!contextChunks.isEmpty()) {
                try {
                    List<Map<String, Object>> chunkInfo = contextChunks.stream()
                            .map(chunk -> {
                                Map<String, Object> info = new HashMap<>();
                                info.put("id", chunk.getId());
                                info.put("content", chunk.getContent().substring(0, Math.min(200, chunk.getContent().length())));
                                info.put("documentId", chunk.getDocument().getId());
                                return info;
                            })
                            .collect(Collectors.toList());
                    history.setContextChunks(objectMapper.writeValueAsString(chunkInfo));
                } catch (JsonProcessingException e) {
                    logger.warn("Failed to serialize context chunks", e);
                }
            }
            
            // 保存Token使用信息
            Map<String, Object> tokenUsage = new HashMap<>();
            tokenUsage.put("promptTokens", estimateTokens(conversationContext));
            tokenUsage.put("completionTokens", estimateTokens(characterResponse));
            tokenUsage.put("totalTokens", estimateTokens(conversationContext) + estimateTokens(characterResponse));
            
            try {
                history.setTokenUsage(objectMapper.writeValueAsString(tokenUsage));
            } catch (JsonProcessingException e) {
                logger.warn("Failed to serialize token usage", e);
            }
            
            RolePlayHistory savedHistory = historyRepository.save(history);
            
            // 更新会话信息
            updateSessionActivity(session);
            
            logger.info("Message processed successfully for session: {}, turn: {}", sessionId, turnNumber);
            return savedHistory;
            
        } catch (Exception e) {
            logger.error("Failed to process message for session: {}", sessionId, e);
            throw new RuntimeException("消息处理失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从知识库检索相关上下文
     */
    private List<DocumentChunk> retrieveRelevantContext(String userMessage, Character character, List<RolePlayHistory> recentHistory) {
        logger.debug("Retrieving relevant context for message: {}", userMessage.substring(0, Math.min(50, userMessage.length())));
        
        try {
            // 构建检索查询，结合用户消息和最近的对话历史
            StringBuilder queryBuilder = new StringBuilder(userMessage);
            
            // 添加最近的对话上下文
            if (!recentHistory.isEmpty()) {
                queryBuilder.append(" ");
                recentHistory.stream()
                        .limit(3) // 只使用最近3轮对话
                        .forEach(h -> queryBuilder.append(h.getUserMessage()).append(" "));
            }
            
            String query = queryBuilder.toString();
            
            // 从知识库检索相关片段
            List<DocumentChunk> chunks = hybridRetrievalService.hybridSearch(
                    query,
                    character.getKnowledgeBase().getId()
            );
            
            // 限制返回的片段数量为5个
            if (chunks.size() > 5) {
                chunks = chunks.subList(0, 5);
            }
            
            logger.debug("Retrieved {} context chunks", chunks.size());
            return chunks;
            
        } catch (Exception e) {
            logger.warn("Failed to retrieve context chunks, continuing without RAG", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 构建对话上下文
     */
    private String buildConversationContext(CharacterProfile profile, List<RolePlayHistory> recentHistory, 
                                          List<DocumentChunk> contextChunks, String userMessage) {
        StringBuilder contextBuilder = new StringBuilder();
        
        // 添加系统提示词
        contextBuilder.append("系统提示词:\n").append(profile.getSystemPrompt()).append("\n\n");
        
        // 添加角色背景信息
        if (profile.getBackgroundStory() != null && !profile.getBackgroundStory().isEmpty()) {
            contextBuilder.append("角色背景:\n").append(profile.getBackgroundStory()).append("\n\n");
        }
        
        // 添加知识库上下文
        if (!contextChunks.isEmpty()) {
            contextBuilder.append("相关知识库内容:\n");
            for (DocumentChunk chunk : contextChunks) {
                contextBuilder.append("- ").append(chunk.getContent()).append("\n");
            }
            contextBuilder.append("\n");
        }
        
        // 添加对话历史
        if (!recentHistory.isEmpty()) {
            contextBuilder.append("对话历史:\n");
            for (RolePlayHistory history : recentHistory) {
                contextBuilder.append("用户: ").append(history.getUserMessage()).append("\n");
                contextBuilder.append(profile.getCharacter().getName()).append(": ").append(history.getCharacterResponse()).append("\n");
            }
            contextBuilder.append("\n");
        }
        
        // 添加当前用户消息
        contextBuilder.append("当前用户消息:\n").append(userMessage).append("\n\n");
        
        // 添加回复指导
        contextBuilder.append("请以").append(profile.getCharacter().getName()).append("的身份回复用户消息。");
        if (profile.getSpeakingStyle() != null && !profile.getSpeakingStyle().isEmpty()) {
            contextBuilder.append("说话风格: ").append(profile.getSpeakingStyle());
        }
        
        return contextBuilder.toString();
    }
    
    /**
     * 生成角色回复
     */
    private String generateCharacterResponse(String conversationContext, CharacterProfile profile) {
        logger.debug("Generating character response using AI model");
        
        try {
            // 构建完整的提示词
            String fullPrompt = buildRolePlayPrompt(conversationContext, profile);
            
            // 调用AI模型生成回复
            String response = chatLanguageModel.generate(fullPrompt);
            
            logger.debug("AI response generated successfully, length: {}", response.length());
            return response;
            
        } catch (Exception e) {
            logger.error("Failed to generate AI response, falling back to default", e);
            
            // 如果AI调用失败，使用备用回复
            String characterName = profile.getCharacter().getName();
            return String.format("【%s】抱歉，我现在有些困惑，能否换个方式问我呢？", characterName);
        }
    }
    
    /**
     * 构建角色扮演的完整提示词
     */
    private String buildRolePlayPrompt(String conversationContext, CharacterProfile profile) {
        StringBuilder promptBuilder = new StringBuilder();
        
        // 1. 系统提示词（角色人设）
        promptBuilder.append("# 角色设定\n");
        promptBuilder.append(profile.getSystemPrompt()).append("\n\n");
        
        // 2. 角色详细信息
        if (profile.getPersonalityTraits() != null) {
            promptBuilder.append("## 性格特征\n");
            promptBuilder.append(profile.getPersonalityTraits()).append("\n\n");
        }
        
        if (profile.getSpeakingStyle() != null) {
            promptBuilder.append("## 说话风格\n");
            promptBuilder.append(profile.getSpeakingStyle()).append("\n\n");
        }
        
        if (profile.getBackgroundStory() != null) {
            promptBuilder.append("## 背景故事\n");
            promptBuilder.append(profile.getBackgroundStory()).append("\n\n");
        }
        
        if (profile.getRestrictions() != null) {
            promptBuilder.append("## 行为限制\n");
            promptBuilder.append(profile.getRestrictions()).append("\n\n");
        }
        
        // 3. 对话上下文和知识库内容
        promptBuilder.append("# 对话上下文\n");
        promptBuilder.append(conversationContext).append("\n\n");
        
        // 4. 指令
        promptBuilder.append("# 指令\n");
        promptBuilder.append("请严格按照上述角色设定进行回复，保持角色一致性。");
        promptBuilder.append("回复应该自然、符合角色特点，不要提及你是AI或角色扮演。");
        promptBuilder.append("直接以角色身份回复，不需要添加角色名称前缀。\n");
        
        return promptBuilder.toString();
    }
    
    /**
     * 获取最近的对话历史
     */
    private List<RolePlayHistory> getRecentHistory(RolePlaySession session, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<RolePlayHistory> histories = historyRepository.findRecentHistoryBySession(session, pageable);
        
        // 按轮次正序排列（最早的在前）
        Collections.reverse(histories);
        return histories;
    }
    
    /**
     * 获取下一个轮次号
     */
    private Integer getNextTurnNumber(RolePlaySession session) {
        Integer maxTurn = historyRepository.findMaxTurnNumberBySession(session);
        return maxTurn != null ? maxTurn + 1 : 1;
    }
    
    /**
     * 更新会话活动时间
     */
    private void updateSessionActivity(RolePlaySession session) {
        session.setLastActivityAt(LocalDateTime.now());
        session.setMessageCount(session.getMessageCount() + 1);
        sessionRepository.save(session);
    }
    
    /**
     * 估算Token数量（简单实现）
     */
    private int estimateTokens(String text) {
        // 简单估算：英文约4个字符=1个token，中文约1.5个字符=1个token
        if (text == null) return 0;
        
        int chineseChars = 0;
        int otherChars = 0;
        
        for (char c : text.toCharArray()) {
            if (c >= 0x4e00 && c <= 0x9fff) {
                chineseChars++;
            } else {
                otherChars++;
            }
        }
        
        return (int) (chineseChars / 1.5 + otherChars / 4.0);
    }
    
    /**
     * 生成会话ID
     */
    private String generateSessionId() {
        return "rp_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * 根据ID和用户获取会话
     */
    @Transactional(readOnly = true)
    public RolePlaySession getSessionByIdAndUser(String sessionId, User user) {
        return sessionRepository.findBySessionIdAndUser(sessionId, user)
                .orElseThrow(() -> new IllegalArgumentException("会话不存在或无权限访问"));
    }
    
    /**
     * 获取用户的会话列表
     */
    @Transactional(readOnly = true)
    public List<RolePlaySession> getUserSessions(User user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return sessionRepository.findByUserOrderByLastActivityAtDesc(user, pageable).getContent();
    }
    
    /**
     * 获取会话的对话历史
     */
    @Transactional(readOnly = true)
    public List<RolePlayHistory> getSessionHistory(User user, String sessionId, int page, int size) {
        RolePlaySession session = getSessionByIdAndUser(sessionId, user);
        Pageable pageable = PageRequest.of(page, size);
        return historyRepository.findByRolePlaySessionOrderByTurnNumberAsc(session, pageable).getContent();
    }
    
    /**
     * 结束会话
     */
    public void endSession(User user, String sessionId) {
        logger.info("Ending session: {} for user: {}", sessionId, user.getUsername());
        
        RolePlaySession session = getSessionByIdAndUser(sessionId, user);
        session.setStatus(RolePlaySession.SessionStatus.ENDED);
        session.setUpdatedAt(LocalDateTime.now());
        
        sessionRepository.save(session);
        logger.info("Session ended: {}", sessionId);
    }
    
    /**
     * 删除会话及其历史记录
     */
    public void deleteSession(User user, String sessionId) {
        logger.info("Deleting session: {} for user: {}", sessionId, user.getUsername());
        
        RolePlaySession session = getSessionByIdAndUser(sessionId, user);
        
        // 删除对话历史
        historyRepository.deleteByRolePlaySession(session);
        
        // 删除会话
        sessionRepository.delete(session);
        
        logger.info("Session deleted: {}", sessionId);
    }
    
    /**
     * 为对话评分
     */
    public void rateConversation(User user, Long historyId, Integer rating, String feedback) {
        logger.info("Rating conversation: {} by user: {}", historyId, user.getUsername());
        
        RolePlayHistory history = historyRepository.findById(historyId)
                .orElseThrow(() -> new IllegalArgumentException("对话记录不存在"));
        
        // 验证权限
        if (!history.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("无权限访问此对话记录");
        }
        
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("评分必须在1-5之间");
        }
        
        history.setUserRating(rating);
        history.setUserFeedback(feedback);
        
        historyRepository.save(history);
        logger.info("Conversation rated: {} with rating: {}", historyId, rating);
    }
    
    /**
     * 获取角色的对话统计
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getCharacterStats(User user, Long characterId) {
        Character character = characterService.getCharacterByIdAndUser(characterId, user);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSessions", sessionRepository.countByCharacter(character));
        stats.put("totalConversations", historyRepository.countByCharacter(character));
        stats.put("activeSessions", sessionRepository.findByCharacterOrderByCreatedAtDesc(character)
                .stream()
                .filter(s -> s.getStatus() == RolePlaySession.SessionStatus.ACTIVE)
                .count());
        
        return stats;
    }
}