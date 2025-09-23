package com.example.ragone.service;

import com.example.ragone.dto.ChatMessage;
import com.example.ragone.dto.ChatSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 聊天缓存服务
 */
@Service
public class ChatCacheService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatCacheService.class);
    
    private static final String SESSION_PREFIX = "chat:session:";
    private static final String USER_SESSIONS_PREFIX = "chat:user:";
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${app.chat.cache.max-conversation-turns:10}")
    private int maxConversationTurns;
    
    @Value("${app.chat.cache.ttl-hours:24}")
    private int cacheTtlHours;
    
    @Value("${app.chat.cache.enabled:true}")
    private boolean cacheEnabled;
    
    /**
     * 创建新的聊天会话
     */
    public String createSession(Long userId, Long knowledgeBaseId, String knowledgeBaseName) {
        if (!cacheEnabled) {
            return UUID.randomUUID().toString();
        }
        
        String sessionId = UUID.randomUUID().toString();
        ChatSession session = new ChatSession(sessionId, userId, knowledgeBaseId);
        session.setKnowledgeBaseName(knowledgeBaseName);
        
        try {
            // 存储会话
            String sessionKey = SESSION_PREFIX + sessionId;
            redisTemplate.opsForValue().set(sessionKey, session, Duration.ofHours(cacheTtlHours));
            
            // 存储用户会话列表
            String userSessionsKey = USER_SESSIONS_PREFIX + userId;
            redisTemplate.opsForSet().add(userSessionsKey, sessionId);
            redisTemplate.expire(userSessionsKey, Duration.ofHours(cacheTtlHours));
            
            logger.info("创建聊天会话: {} for 用户: {}", sessionId, userId);
            return sessionId;
            
        } catch (Exception e) {
            logger.error("创建聊天会话失败", e);
            return sessionId; // 即使缓存失败也返回sessionId
        }
    }
    
    /**
     * 获取聊天会话
     */
    public ChatSession getSession(String sessionId) {
        if (!cacheEnabled) {
            return null;
        }
        
        try {
            String sessionKey = SESSION_PREFIX + sessionId;
            Object sessionObj = redisTemplate.opsForValue().get(sessionKey);
            
            if (sessionObj instanceof ChatSession) {
                return (ChatSession) sessionObj;
            }
            
            return null;
            
        } catch (Exception e) {
            logger.error("获取聊天会话失败: {}", sessionId, e);
            return null;
        }
    }
    
    /**
     * 更新聊天会话
     */
    public void updateSession(ChatSession session) {
        if (!cacheEnabled || session == null) {
            return;
        }
        
        try {
            String sessionKey = SESSION_PREFIX + session.getSessionId();
            redisTemplate.opsForValue().set(sessionKey, session, Duration.ofHours(cacheTtlHours));
            
            logger.debug("更新聊天会话: {}", session.getSessionId());
            
        } catch (Exception e) {
            logger.error("更新聊天会话失败: {}", session.getSessionId(), e);
        }
    }
    
    /**
     * 添加消息到会话
     */
    public void addMessage(String sessionId, ChatMessage message) {
        if (!cacheEnabled) {
            return;
        }
        
        try {
            ChatSession session = getSession(sessionId);
            if (session != null) {
                session.addMessage(message);
                
                // 限制消息数量
                if (session.getMessages().size() > maxConversationTurns * 2) { // 用户+助手消息
                    List<ChatMessage> messages = session.getMessages();
                    // 保留最新的消息，删除最旧的
                    int removeCount = messages.size() - maxConversationTurns * 2;
                    for (int i = 0; i < removeCount; i++) {
                        messages.remove(0);
                    }
                    session.setMessages(messages);
                }
                
                updateSession(session);
            }
            
        } catch (Exception e) {
            logger.error("添加消息到会话失败: {}", sessionId, e);
        }
    }
    
    /**
     * 添加用户消息
     */
    public void addUserMessage(String sessionId, String content) {
        addMessage(sessionId, new ChatMessage("user", content));
    }
    
    /**
     * 添加助手消息
     */
    public void addAssistantMessage(String sessionId, String content, String contextChunks) {
        addMessage(sessionId, new ChatMessage("assistant", content, contextChunks));
    }
    
    /**
     * 获取会话的聊天历史（用于上下文）
     */
    public String getConversationHistory(String sessionId) {
        if (!cacheEnabled) {
            return "";
        }
        
        try {
            ChatSession session = getSession(sessionId);
            if (session == null || session.getMessages().isEmpty()) {
                return "";
            }
            
            StringBuilder history = new StringBuilder();
            List<ChatMessage> messages = session.getMessages();//从session中获取历史聊天记录
            
            // 只取最近的几轮对话作为上下文
            int startIndex = Math.max(0, messages.size() - maxConversationTurns * 2);
            
            for (int i = startIndex; i < messages.size(); i++) {
                ChatMessage message = messages.get(i);
                if ("user".equals(message.getRole())) {
                    history.append("用户: ").append(message.getContent()).append("\n");
                } else if ("assistant".equals(message.getRole())) {
                    history.append("助手: ").append(message.getContent()).append("\n");
                }
            }
            
            return history.toString();
            
        } catch (Exception e) {
            logger.error("获取会话历史失败: {}", sessionId, e);
            return "";
        }
    }
    
    /**
     * 删除会话
     */
    public void deleteSession(String sessionId) {
        if (!cacheEnabled) {
            return;
        }
        
        try {
            String sessionKey = SESSION_PREFIX + sessionId;
            redisTemplate.delete(sessionKey);
            
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
            return List.of();
        }
        
        try {
            String userSessionsKey = USER_SESSIONS_PREFIX + userId;
            return redisTemplate.opsForSet().members(userSessionsKey)
                    .stream()
                    .map(Object::toString)
                    .toList();
                    
        } catch (Exception e) {
            logger.error("获取用户会话列表失败: {}", userId, e);
            return List.of();
        }
    }
    
    /**
     * 清理过期会话
     */
    public void cleanupExpiredSessions(Long userId) {
        if (!cacheEnabled) {
            return;
        }
        
        try {
            List<String> sessionIds = getUserSessions(userId);
            for (String sessionId : sessionIds) {
                ChatSession session = getSession(sessionId);
                if (session == null) {
                    // 会话已过期，从用户会话列表中移除
                    String userSessionsKey = USER_SESSIONS_PREFIX + userId;
                    redisTemplate.opsForSet().remove(userSessionsKey, sessionId);
                }
            }
            
        } catch (Exception e) {
            logger.error("清理过期会话失败: {}", userId, e);
        }
    }
}

