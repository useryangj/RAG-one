package com.example.ragone.service;

import com.example.ragone.dto.ChatSession;
import com.example.ragone.entity.ChatHistory;
import com.example.ragone.entity.DocumentChunk;
import com.example.ragone.entity.KnowledgeBase;
import com.example.ragone.entity.User;
import com.example.ragone.repository.ChatHistoryRepository;
import com.example.ragone.repository.DocumentChunkRepository;
import com.example.ragone.repository.KnowledgeBaseRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG问答服务
 */
@Service
public class RagService {
    
    private static final Logger logger = LoggerFactory.getLogger(RagService.class);
    
    @Autowired
    private ChatLanguageModel chatLanguageModel;
    
    @Autowired
    private EmbeddingModel embeddingModel;
    
    @Autowired
    private DocumentChunkRepository documentChunkRepository;
    
    @Autowired
    private KnowledgeBaseRepository knowledgeBaseRepository;
    
    @Autowired
    private ChatCacheService chatCacheService;
    
    @Autowired
    private ChatHistoryRepository chatHistoryRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${app.chat.cache.enabled:true}")
    private boolean cacheEnabled;
    
    /**
     * 基于知识库进行问答（带会话支持）
     */
    public String askQuestion(String question, Long knowledgeBaseId, User user, String sessionId) {
        try {
            // 验证知识库权限
            KnowledgeBase knowledgeBase = knowledgeBaseRepository.findByIdAndUser(knowledgeBaseId, user)
                    .orElseThrow(() -> new RuntimeException("知识库不存在或无权访问"));
            
            // 获取或创建会话
            getOrCreateSession(sessionId, user, knowledgeBase);
            
            // 获取聊天历史上下文
            String conversationHistory = "";
            if (cacheEnabled && sessionId != null) {
                conversationHistory = chatCacheService.getConversationHistory(sessionId);
            }
            
            // 1. 将问题转换为向量
            Embedding questionEmbedding = embeddingModel.embed(question).content();
            String embeddingString = embeddingToString(questionEmbedding);
            
            // 2. 在知识库中搜索相关文档片段
            List<DocumentChunk> relevantChunks = documentChunkRepository.findSimilarChunks(
                    knowledgeBaseId, embeddingString, 5);
            
            if (relevantChunks.isEmpty()) {
                String response = "抱歉，在您的知识库中没有找到相关信息。";
                // 保存到缓存和数据库
                saveChatInteraction(sessionId, question, response, null, user, knowledgeBase, 0L);
                return response;
            }
            
            // 3. 构建上下文
            String context = relevantChunks.stream()
                    .map(DocumentChunk::getContent)
                    .collect(Collectors.joining("\n\n"));
            
            // 4. 构建提示词（包含聊天历史）
            String prompt = buildPromptWithHistory(context, question, conversationHistory);
            
            // 5. 调用大模型生成回答
            long startTime = System.currentTimeMillis();
            String response = chatLanguageModel.generate(prompt);
            long responseTime = System.currentTimeMillis() - startTime;
            
            // 6. 保存到缓存和数据库
            String contextChunksJson = convertChunksToJson(relevantChunks);
            saveChatInteraction(sessionId, question, response, contextChunksJson, user, knowledgeBase, responseTime);
            
            logger.info("用户 {} 在知识库 {} 中提问: {} (会话: {})", user.getUsername(), knowledgeBase.getName(), question, sessionId);
            
            return response;
            
        } catch (Exception e) {
            logger.error("RAG问答失败", e);
            return "抱歉，处理您的问题时出现了错误。请稍后再试。";
        }
    }
    
    /**
     * 基于知识库进行问答（兼容旧接口）
     */
    public String askQuestion(String question, Long knowledgeBaseId, User user) {
        return askQuestion(question, knowledgeBaseId, user, null);
    }
    
    /**
     * 获取或创建聊天会话
     */
    private ChatSession getOrCreateSession(String sessionId, User user, KnowledgeBase knowledgeBase) {
        if (sessionId == null || !cacheEnabled) {
            return null;
        }
        
        ChatSession session = chatCacheService.getSession(sessionId);
        if (session == null) {
            // 创建新会话
            sessionId = chatCacheService.createSession(user.getId(), knowledgeBase.getId(), knowledgeBase.getName());
            session = chatCacheService.getSession(sessionId);
        }
        
        return session;
    }
    
    /**
     * 保存聊天交互到缓存和数据库
     */
    private void saveChatInteraction(String sessionId, String question, String response, 
                                   String contextChunks, User user, KnowledgeBase knowledgeBase, 
                                   Long responseTime) {
        try {
            // 保存到Redis缓存
            if (cacheEnabled && sessionId != null) {
                chatCacheService.addUserMessage(sessionId, question);
                chatCacheService.addAssistantMessage(sessionId, response, contextChunks);
            }
            
            // 保存到数据库
            ChatHistory chatHistory = new ChatHistory();
            chatHistory.setSessionId(sessionId);
            chatHistory.setUser(user);
            chatHistory.setKnowledgeBase(knowledgeBase);
            chatHistory.setUserMessage(question);
            chatHistory.setAssistantResponse(response);
            chatHistory.setContextChunks(contextChunks);
            chatHistory.setResponseTimeMs(responseTime);
            chatHistory.setCreatedAt(LocalDateTime.now());
            
            chatHistoryRepository.save(chatHistory);
            
        } catch (Exception e) {
            logger.error("保存聊天交互失败", e);
        }
    }
    
    /**
     * 将文档片段转换为JSON
     */
    private String convertChunksToJson(List<DocumentChunk> chunks) {
        try {
            return objectMapper.writeValueAsString(chunks.stream()
                    .map(chunk -> {
                        // 创建一个简化的对象用于JSON序列化
                        return new Object() {
                            @SuppressWarnings("unused")
                            public final Long id = chunk.getId();
                            @SuppressWarnings("unused")
                            public final String content = chunk.getContent();
                            @SuppressWarnings("unused")
                            public final Integer chunkPosition = chunk.getChunkPosition();
                        };
                    })
                    .toList());
        } catch (JsonProcessingException e) {
            logger.error("转换文档片段为JSON失败", e);
            return "[]";
        }
    }
    
    /**
     * 构建包含聊天历史的提示词
     */
    private String buildPromptWithHistory(String context, String question, String conversationHistory) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请基于以下知识库内容和聊天历史回答用户的问题。如果知识库中没有相关信息，请明确说明。\n\n");
        
        if (!conversationHistory.isEmpty()) {
            prompt.append("聊天历史：\n");
            prompt.append(conversationHistory);
            prompt.append("\n\n");
        }
        
        prompt.append("知识库内容：\n");
        prompt.append(context);
        prompt.append("\n\n");
        
        prompt.append("用户问题：");
        prompt.append(question);
        prompt.append("\n\n");
        
        prompt.append("请提供准确、有用的回答，并考虑聊天历史中的上下文：");
        
        return prompt.toString();
    }
    
    /**
     * 构建提示词（原方法保持不变）
     */
    private String buildPrompt(String context, String question) {
        return String.format("""
                请基于以下知识库内容回答用户的问题。如果知识库中没有相关信息，请明确说明。
                
                知识库内容：
                %s
                
                用户问题：%s
                
                请提供准确、有用的回答：
                """, context, question);
    }
    
    /**
     * 创建新的聊天会话
     */
    public String createSession(Long userId, Long knowledgeBaseId) {
        if (!cacheEnabled) {
            return null;
        }
        
        try {
            KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(knowledgeBaseId)
                    .orElseThrow(() -> new RuntimeException("知识库不存在"));
            
            return chatCacheService.createSession(userId, knowledgeBaseId, knowledgeBase.getName());
        } catch (Exception e) {
            logger.error("创建聊天会话失败", e);
            return null;
        }
    }
    
    /**
     * 获取聊天会话
     */
    public ChatSession getSession(String sessionId) {
        if (!cacheEnabled) {
            return null;
        }
        
        return chatCacheService.getSession(sessionId);
    }
    
    /**
     * 获取用户的聊天历史
     */
    public List<ChatHistory> getChatHistory(Long userId, String sessionId) {
        if (sessionId != null) {
            return chatHistoryRepository.findByUserIdAndSessionIdOrderByCreatedAtAsc(userId, sessionId);
        } else {
            return chatHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId,
                    org.springframework.data.domain.PageRequest.of(0, 50)).getContent();
        }
    }
    
    /**
     * 删除聊天会话
     */
    public void deleteSession(String sessionId) {
        if (!cacheEnabled) {
            return;
        }
        
        try {
            // 从Redis删除
            chatCacheService.deleteSession(sessionId);
            
            // 从数据库删除
            chatHistoryRepository.deleteBySessionId(sessionId);
            
            logger.info("删除聊天会话: {}", sessionId);
        } catch (Exception e) {
            logger.error("删除聊天会话失败: {}", sessionId, e);
        }
    }
    
    /**
     * 获取用户的所有会话
     */
    public List<String> getUserSessions(Long userId) {
        if (!cacheEnabled) {
            return chatHistoryRepository.findDistinctSessionIdsByUserId(userId);
        }
        
        return chatCacheService.getUserSessions(userId);
    }
    
    /**
     * 将Embedding转换为字符串格式（用于数据库存储）
     */
    private String embeddingToString(Embedding embedding) {
        float[] vector = embedding.vector();
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
