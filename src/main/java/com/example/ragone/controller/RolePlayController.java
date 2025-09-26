package com.example.ragone.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ragone.entity.RolePlayHistory;
import com.example.ragone.entity.RolePlaySession;
import com.example.ragone.entity.User;
import com.example.ragone.service.RolePlayService;

/**
 * 角色扮演控制器
 */
@RestController
@RequestMapping("/roleplay")
@CrossOrigin(origins = "*", maxAge = 3600)
public class RolePlayController {

    private static final Logger log = LoggerFactory.getLogger(RolePlayController.class);
    @Autowired
    private RolePlayService rolePlayService;
    
    /**
     * 开始角色扮演会话
     */
    @PostMapping("/start")
    public ResponseEntity<?> startRolePlaySession(@RequestBody Map<String, Object> request,
                                                 Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            Long characterId = Long.valueOf(request.get("characterId").toString());
            // 兼容前端的sessionName字段，如果没有则使用scenario
            String sessionName = (String) request.getOrDefault("sessionName", 
                                 (String) request.get("scenario"));
            
            RolePlaySession session = rolePlayService.createSession(user, characterId, sessionName);
            return ResponseEntity.ok(session);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 发送消息
     */
    @PostMapping("/message")
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, Object> request,
                                       Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            String sessionId = (String) request.get("sessionId");
            String message = (String) request.get("message");
            
            RolePlayHistory history = rolePlayService.sendMessage(user, sessionId, message);
            
            // 构建符合前端期望的响应格式
            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", sessionId);
            response.put("userMessage", history.getUserMessage());
            response.put("characterResponse", history.getCharacterResponse());
            response.put("responseTimeMs", history.getResponseTimeMs());
            response.put("tokenUsage", extractTokenUsage(history.getTokenUsage()));
            response.put("usedRag", history.getUsedRag());
            response.put("retrievedDocumentCount", history.getRetrievedChunksCount());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("sendMessage error", e);
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 从JSON字符串中提取token使用量
     */
    private int extractTokenUsage(String tokenUsageJson) {
        try {
            if (tokenUsageJson == null) return 0;
            
            ObjectMapper mapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> tokenData = mapper.readValue(tokenUsageJson, Map.class);
            Object totalTokens = tokenData.get("totalTokens");
            
            if (totalTokens instanceof Number) {
                return ((Number) totalTokens).intValue();
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * 获取用户的会话列表
     */
    @GetMapping("/sessions")
    public ResponseEntity<List<RolePlaySession>> getUserSessions(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<RolePlaySession> sessions = rolePlayService.getUserSessions(user, 0, 100);
        return ResponseEntity.ok(sessions);
    }
    
    /**
     * 获取会话详情
     */
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<?> getSession(@PathVariable String sessionId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            RolePlaySession session = rolePlayService.getSessionByIdAndUser(sessionId, user);
            return ResponseEntity.ok(session);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取会话历史消息
     */
    @GetMapping("/sessions/{sessionId}/history")
    public ResponseEntity<List<RolePlayHistory>> getSessionHistory(@PathVariable String sessionId,
                                                                  Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<RolePlayHistory> history = rolePlayService.getSessionHistory(user, sessionId, 0, 100);
        return ResponseEntity.ok(history);
    }
    
    /**
     * 结束会话
     */
    @PatchMapping("/sessions/{sessionId}/end")
    public ResponseEntity<?> endSession(@PathVariable String sessionId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            rolePlayService.endSession(user, sessionId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "会话已结束");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 删除会话
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<?> deleteSession(@PathVariable String sessionId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            rolePlayService.deleteSession(user, sessionId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "会话删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 为消息评分
     */
    @PatchMapping("/messages/{messageId}/rate")
    public ResponseEntity<?> rateMessage(@PathVariable String messageId,
                                       @RequestBody Map<String, Object> request,
                                       Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            Integer rating = (Integer) request.get("rating");
            Long historyId = Long.valueOf(messageId);
            rolePlayService.rateConversation(user, historyId, rating, null);
            Map<String, String> response = new HashMap<>();
            response.put("message", "评分成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
