package com.example.ragone.controller;

import com.example.ragone.entity.KnowledgeBase;
import com.example.ragone.entity.User;
import com.example.ragone.service.KnowledgeBaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库控制器
 */
@RestController
@RequestMapping("/knowledge-bases")
@CrossOrigin(origins = "*", maxAge = 3600)
public class KnowledgeBaseController {
    
    @Autowired
    private KnowledgeBaseService knowledgeBaseService;
    
    /**
     * 创建知识库
     */
    @PostMapping
    public ResponseEntity<?> createKnowledgeBase(@RequestParam String name,
                                               @RequestParam(required = false) String description,
                                               Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            KnowledgeBase knowledgeBase = knowledgeBaseService.createKnowledgeBase(name, description, user);
            return ResponseEntity.ok(knowledgeBase);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取用户的所有知识库
     */
    @GetMapping
    public ResponseEntity<List<KnowledgeBase>> getUserKnowledgeBases(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<KnowledgeBase> knowledgeBases = knowledgeBaseService.getUserKnowledgeBases(user);
        return ResponseEntity.ok(knowledgeBases);
    }
    
    /**
     * 获取知识库详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getKnowledgeBase(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            KnowledgeBase knowledgeBase = knowledgeBaseService.getKnowledgeBase(id, user);
            return ResponseEntity.ok(knowledgeBase);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 更新知识库
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateKnowledgeBase(@PathVariable Long id,
                                               @RequestParam String name,
                                               @RequestParam(required = false) String description,
                                               Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            KnowledgeBase knowledgeBase = knowledgeBaseService.updateKnowledgeBase(id, name, description, user);
            return ResponseEntity.ok(knowledgeBase);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 删除知识库
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteKnowledgeBase(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            knowledgeBaseService.deleteKnowledgeBase(id, user);
            Map<String, String> response = new HashMap<>();
            response.put("message", "知识库删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
