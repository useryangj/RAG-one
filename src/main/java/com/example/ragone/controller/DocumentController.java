package com.example.ragone.controller;

import com.example.ragone.entity.Document;
import com.example.ragone.entity.User;
import com.example.ragone.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档控制器
 */
@RestController
@RequestMapping("/documents")
@CrossOrigin(origins = "*", maxAge = 3600)
public class DocumentController {
    
    @Autowired
    private DocumentService documentService;
    
    /**
     * 上传文档
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(@RequestParam("file") MultipartFile file,
                                          @RequestParam("knowledgeBaseId") Long knowledgeBaseId,
                                          Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            // 验证文件
            if (file.isEmpty()) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "请选择文件");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 检查文件类型
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.equals("application/pdf") 
                && !contentType.startsWith("text/")
                && !contentType.equals("application/msword")
                && !contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "不支持的文件类型，请上传PDF、Word或文本文件");
                return ResponseEntity.badRequest().body(response);
            }
            
            Document document = documentService.uploadDocument(file, knowledgeBaseId, user);
            return ResponseEntity.ok(document);
            
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取知识库的文档列表
     */
    @GetMapping
    public ResponseEntity<?> getDocuments(@RequestParam Long knowledgeBaseId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            List<Document> documents = documentService.getUserDocuments(knowledgeBaseId, user);
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 删除文档
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            documentService.deleteDocument(id, user);
            Map<String, String> response = new HashMap<>();
            response.put("message", "文档删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
