package com.example.ragone.controller;

import com.example.ragone.entity.User;
import com.example.ragone.service.RagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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
     * 问答接口
     */
    @PostMapping("/ask")
    public ResponseEntity<?> askQuestion(@RequestParam String question,
                                       @RequestParam Long knowledgeBaseId,
                                       Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        long startTime = System.currentTimeMillis();
        String answer = ragService.askQuestion(question, knowledgeBaseId, user);
        long responseTime = System.currentTimeMillis() - startTime;
        
        Map<String, Object> response = new HashMap<>();
        response.put("question", question);
        response.put("answer", answer);
        response.put("knowledgeBaseId", knowledgeBaseId);
        response.put("responseTimeMs", responseTime);
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
}
