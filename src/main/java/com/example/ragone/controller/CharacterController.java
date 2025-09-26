package com.example.ragone.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.ragone.dto.CharacterDto;
import com.example.ragone.entity.Character;
import com.example.ragone.entity.User;
import com.example.ragone.service.CharacterService;

/**
 * 角色管理控制器
 */
@RestController
@RequestMapping("/characters")
@CrossOrigin(origins = "*", maxAge = 3600)
public class CharacterController {
    
    @Autowired
    private CharacterService characterService;
    
    /**
     * 创建角色
     */
    @PostMapping
    public ResponseEntity<?> createCharacter(@RequestBody Map<String, Object> request,
                                           Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            String name = (String) request.get("name");
            String description = (String) request.get("description");
            String avatarUrl = (String) request.get("avatarUrl");
            Long knowledgeBaseId = Long.valueOf(request.get("knowledgeBaseId").toString());
            Boolean isPublic = (Boolean) request.getOrDefault("isPublic", false);
            
            Character character = characterService.createCharacter(user, name, description, 
                                                                 knowledgeBaseId, avatarUrl, isPublic);
            return ResponseEntity.ok(character);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取用户的所有角色
     */
    @GetMapping
    public ResponseEntity<List<CharacterDto>> getUserCharacters(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<Character> characters = characterService.getUserCharacters(user, 0, 100).getContent();
        
        // 转换为DTO以避免序列化问题
        List<CharacterDto> characterDtos = characters.stream()
                .map(CharacterDto::new)
                .collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(characterDtos);
    }
    
    /**
     * 根据知识库获取角色
     */
    @GetMapping("/knowledge-base/{knowledgeBaseId}")
    public ResponseEntity<List<CharacterDto>> getCharactersByKnowledgeBase(@PathVariable Long knowledgeBaseId,
                                                                       Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<Character> characters = characterService.getCharactersByKnowledgeBase(user, knowledgeBaseId);
        
        // 转换为DTO以避免序列化问题
        List<CharacterDto> characterDtos = characters.stream()
                .map(CharacterDto::new)
                .collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(characterDtos);
    }
    
    /**
     * 获取角色详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getCharacter(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            Character character = characterService.getCharacterByIdAndUser(id, user);
            return ResponseEntity.ok(character);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 更新角色
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCharacter(@PathVariable Long id,
                                           @RequestBody Map<String, Object> request,
                                           Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            String name = (String) request.get("name");
            String description = (String) request.get("description");
            String avatarUrl = (String) request.get("avatarUrl");
            Boolean isPublic = (Boolean) request.getOrDefault("isPublic", false);
            
            Character character = characterService.updateCharacter(user, id, name, description, avatarUrl, isPublic);
            return ResponseEntity.ok(character);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 删除角色
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCharacter(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            characterService.deleteCharacter(user, id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "角色删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 激活/停用角色
     */
    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<?> toggleCharacterStatus(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            Character character = getCharacterAndToggleStatus(id, user);
            return ResponseEntity.ok(character);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 生成角色配置文件
     */
    @PostMapping("/{id}/generate-profile")
    public ResponseEntity<?> generateCharacterProfile(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            characterService.regenerateCharacterProfile(user, id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "角色配置文件生成中，请稍后查看");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 搜索角色
     */
    @GetMapping("/search")
    public ResponseEntity<List<CharacterDto>> searchCharacters(@RequestParam String query,
                                                          @RequestParam(required = false) Long knowledgeBaseId,
                                                          Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<Character> characters = characterService.searchCharacters(user, query);
        
        // 转换为DTO以避免序列化问题
        List<CharacterDto> characterDtos = characters.stream()
                .map(CharacterDto::new)
                .collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(characterDtos);
    }
    
    /**
     * 辅助方法：获取角色并切换状态
     */
    private Character getCharacterAndToggleStatus(Long id, User user) {
        Character character = characterService.getCharacterByIdAndUser(id, user);
        
        if (character.getStatus() == Character.CharacterStatus.ACTIVE) {
            return characterService.deactivateCharacter(user, id);
        } else {
            return characterService.activateCharacter(user, id);
        }
    }
}
