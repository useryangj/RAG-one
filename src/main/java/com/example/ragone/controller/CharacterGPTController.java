package com.example.ragone.controller;

import com.example.ragone.dto.CharacterGPTConfig;
import com.example.ragone.entity.Character;
import com.example.ragone.entity.CharacterProfile;
import com.example.ragone.entity.User;
import com.example.ragone.service.CharacterGPTTemplateService;
import com.example.ragone.service.CharacterProfileService;
import com.example.ragone.service.CharacterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * CharacterGPT模板控制器
 * 提供CharacterGPT模板生成和配置的API接口
 */
@RestController
@RequestMapping("/api/charactergpt")
@CrossOrigin(origins = "*")
public class CharacterGPTController {
    
    private static final Logger logger = LoggerFactory.getLogger(CharacterGPTController.class);
    
    @Autowired
    private CharacterGPTTemplateService characterGPTTemplateService;
    
    @Autowired
    private CharacterProfileService characterProfileService;
    
    @Autowired
    private CharacterService characterService;
    
    /**
     * 生成CharacterGPT格式的角色提示词
     */
    @PostMapping("/generate/{characterId}")
    public ResponseEntity<?> generateCharacterGPTPrompt(
            @PathVariable Long characterId,
            @RequestBody(required = false) CharacterGPTConfig config,
            Authentication authentication) {
        
        try {
            User user = (User) authentication.getPrincipal();
            
            // 验证角色权限
            Character character = characterService.getCharacterByIdAndUser(characterId, user);
            
            // 使用默认配置如果未提供
            if (config == null) {
                config = CharacterGPTConfig.getStandardConfig();
            }
            
            // 生成CharacterGPT格式的提示词
            String prompt = characterGPTTemplateService.generateCharacterGPTPrompt(
                character, 
                characterProfileService.getCharacterProfile(character).orElse(null),
                null, // 这里可以传入相关的文档片段
                config
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("prompt", prompt);
            response.put("config", config);
            response.put("characterId", characterId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to generate CharacterGPT prompt for character: {}", characterId, e);
            return ResponseEntity.badRequest().body(Map.of("message", "生成CharacterGPT提示词失败: " + e.getMessage()));
        }
    }
    
    /**
     * 重新生成角色的CharacterGPT提示词
     */
    @PostMapping("/regenerate/{characterId}")
    public ResponseEntity<?> regenerateCharacterGPTPrompt(
            @PathVariable Long characterId,
            @RequestBody(required = false) CharacterGPTConfig config,
            Authentication authentication) {
        
        try {
            User user = (User) authentication.getPrincipal();
            
            // 验证角色权限
            Character character = characterService.getCharacterByIdAndUser(characterId, user);
            
            // 重新生成CharacterGPT格式的提示词
            characterProfileService.regenerateCharacterGPTPrompt(character);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "CharacterGPT提示词重新生成成功");
            response.put("characterId", characterId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to regenerate CharacterGPT prompt for character: {}", characterId, e);
            return ResponseEntity.badRequest().body(Map.of("message", "重新生成CharacterGPT提示词失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取预设配置
     */
    @GetMapping("/configs/presets")
    public ResponseEntity<?> getPresetConfigs() {
        Map<String, Object> response = new HashMap<>();
        response.put("standard", CharacterGPTConfig.getStandardConfig());
        response.put("minimal", CharacterGPTConfig.getMinimalConfig());
        response.put("detailed", CharacterGPTConfig.getDetailedConfig());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 验证配置
     */
    @PostMapping("/configs/validate")
    public ResponseEntity<?> validateConfig(@RequestBody CharacterGPTConfig config) {
        try {
            // 基本验证
            if (config.getExampleCount() < 1 || config.getExampleCount() > 10) {
                return ResponseEntity.badRequest().body(Map.of("message", "示例数量必须在1-10之间"));
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("valid", true);
            response.put("message", "配置验证通过");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to validate config", e);
            return ResponseEntity.badRequest().body(Map.of("message", "配置验证失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取角色的CharacterGPT提示词
     */
    @GetMapping("/prompt/{characterId}")
    public ResponseEntity<?> getCharacterGPTPrompt(
            @PathVariable Long characterId,
            Authentication authentication) {
        
        try {
            User user = (User) authentication.getPrincipal();
            
            // 验证角色权限
            Character character = characterService.getCharacterByIdAndUser(characterId, user);
            
            // 获取角色的配置文件
            var profileOpt = characterProfileService.getCharacterProfile(character);
            if (profileOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "角色配置文件不存在"));
            }
            
            CharacterProfile profile = profileOpt.get();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("prompt", profile.getSystemPrompt());
            response.put("characterId", characterId);
            response.put("version", profile.getVersion());
            response.put("updatedAt", profile.getUpdatedAt());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get CharacterGPT prompt for character: {}", characterId, e);
            return ResponseEntity.badRequest().body(Map.of("message", "获取CharacterGPT提示词失败: " + e.getMessage()));
        }
    }
}
