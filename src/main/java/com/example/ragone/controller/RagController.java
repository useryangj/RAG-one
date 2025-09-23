package com.example.ragone.controller;

import com.example.ragone.dto.ChatSession;
import com.example.ragone.entity.ChatHistory;
import com.example.ragone.entity.User;
import com.example.ragone.service.RagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG问答控制器
 */
@RestController
@RequestMapping("/rag")
@CrossOrigin(origins = "*", maxAge = 3600)
public class RagController {
    
    @Autowired
    private RagService ragService;
    
    /**
     * 问答接口（支持会话）
     */
    @PostMapping("/ask")
    public ResponseEntity<?> askQuestion(@RequestParam String question,
                                       @RequestParam Long knowledgeBaseId,
                                       @RequestParam(required = false) String sessionId,
                                       Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        long startTime = System.currentTimeMillis();
        String answer = ragService.askQuestion(question, knowledgeBaseId, user, sessionId);
        long responseTime = System.currentTimeMillis() - startTime;
        
        Map<String, Object> response = new HashMap<>();
        response.put("question", question);
        response.put("answer", answer);
        response.put("knowledgeBaseId", knowledgeBaseId);
        response.put("sessionId", sessionId);
        response.put("responseTimeMs", responseTime);
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 创建新的聊天会话
     */
    @PostMapping("/session/create")
    public ResponseEntity<?> createSession(@RequestParam Long knowledgeBaseId,
                                         Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            String sessionId = ragService.createSession(user.getId(), knowledgeBaseId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", sessionId);
            response.put("knowledgeBaseId", knowledgeBaseId);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "创建会话失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 获取聊天会话信息
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<?> getSession(@PathVariable String sessionId,
                                      Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            ChatSession session = ragService.getSession(sessionId);
            if (session == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "会话不存在");
                return ResponseEntity.notFound().build();
            }
            
            // 验证会话是否属于当前用户
            if (!session.getUserId().equals(user.getId())) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "无权访问此会话");
                return ResponseEntity.status(403).build();
            }
            
            return ResponseEntity.ok(session);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "获取会话失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 获取聊天历史
     */
    @GetMapping("/history")
    public ResponseEntity<?> getChatHistory(@RequestParam(required = false) String sessionId,
                                          Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            List<ChatHistory> history = ragService.getChatHistory(user.getId(), sessionId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "获取聊天历史失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 获取用户的所有会话
     */
    @GetMapping("/sessions")
    public ResponseEntity<?> getUserSessions(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            List<String> sessions = ragService.getUserSessions(user.getId());
            return ResponseEntity.ok(sessions);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "获取会话列表失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 删除聊天会话
     */
    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<?> deleteSession(@PathVariable String sessionId,
                                         Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            // 验证会话是否属于当前用户
            ChatSession session = ragService.getSession(sessionId);
            if (session != null && !session.getUserId().equals(user.getId())) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "无权删除此会话");
                return ResponseEntity.status(403).build();
            }
            
            ragService.deleteSession(sessionId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "会话删除成功");
            response.put("sessionId", sessionId);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "删除会话失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
