package com.example.ragone.service;

import com.example.ragone.entity.Character;
import com.example.ragone.entity.KnowledgeBase;
import com.example.ragone.entity.User;
import com.example.ragone.repository.CharacterRepository;
import com.example.ragone.repository.KnowledgeBaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 角色管理服务类
 */
@Service
@Transactional
public class CharacterService {
    
    private static final Logger logger = LoggerFactory.getLogger(CharacterService.class);
    
    @Autowired
    private CharacterRepository characterRepository;
    
    @Autowired
    private KnowledgeBaseRepository knowledgeBaseRepository;
    
    @Autowired
    private CharacterProfileService characterProfileService;
    
    /**
     * 创建新角色
     */
    public Character createCharacter(User user, String name, String description, 
                                   Long knowledgeBaseId, String avatarUrl, boolean isPublic) {
        logger.info("Creating character for user: {}, name: {}", user.getUsername(), name);
        
        // 检查角色名称是否已存在
        if (characterRepository.existsByUserAndName(user, name)) {
            throw new IllegalArgumentException("角色名称已存在: " + name);
        }
        
        // 验证知识库是否存在且属于用户
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findByIdAndUser(knowledgeBaseId, user)
                .orElseThrow(() -> new IllegalArgumentException("知识库不存在或无权限访问"));
        
        if (!knowledgeBase.getActive()) {
            throw new IllegalArgumentException("知识库未激活，无法创建角色");
        }
        
        // 创建角色实体
        Character character = new Character();
        character.setName(name);
        character.setDescription(description);
        character.setUser(user);
        character.setKnowledgeBase(knowledgeBase);
        character.setAvatarUrl(avatarUrl);
        character.setIsPublic(isPublic);
        character.setStatus(Character.CharacterStatus.DRAFT);
        
        Character savedCharacter = characterRepository.save(character);
        logger.info("Character created successfully with ID: {}", savedCharacter.getId());
        
        // 异步生成角色配置文件
        characterProfileService.generateProfileAsync(savedCharacter);
        
        return savedCharacter;
    }
    
    /**
     * 更新角色信息
     */
    public Character updateCharacter(User user, Long characterId, String name, 
                                   String description, String avatarUrl, Boolean isPublic) {
        logger.info("Updating character ID: {} for user: {}", characterId, user.getUsername());
        
        Character character = getCharacterByIdAndUser(characterId, user);
        
        // 检查新名称是否与其他角色冲突
        if (name != null && !name.equals(character.getName())) {
            if (characterRepository.existsByUserAndName(user, name)) {
                throw new IllegalArgumentException("角色名称已存在: " + name);
            }
            character.setName(name);
        }
        
        if (description != null) {
            character.setDescription(description);
        }
        
        if (avatarUrl != null) {
            character.setAvatarUrl(avatarUrl);
        }
        
        if (isPublic != null) {
            character.setIsPublic(isPublic);
        }
        
        character.setUpdatedAt(LocalDateTime.now());
        
        Character updatedCharacter = characterRepository.save(character);
        logger.info("Character updated successfully: {}", updatedCharacter.getId());
        
        return updatedCharacter;
    }
    
    /**
     * 删除角色
     */
    public void deleteCharacter(User user, Long characterId) {
        logger.info("Deleting character ID: {} for user: {}", characterId, user.getUsername());
        
        Character character = getCharacterByIdAndUser(characterId, user);
        
        // 检查是否有活跃的角色扮演会话
        // TODO: 添加会话检查逻辑
        
        characterRepository.delete(character);
        logger.info("Character deleted successfully: {}", characterId);
    }
    
    /**
     * 激活角色
     */
    public Character activateCharacter(User user, Long characterId) {
        logger.info("Activating character ID: {} for user: {}", characterId, user.getUsername());
        
        Character character = getCharacterByIdAndUser(characterId, user);
        
        // 检查角色配置文件是否完成
        if (!characterProfileService.isProfileCompleted(character)) {
            throw new IllegalStateException("角色配置文件未完成，无法激活");
        }
        
        character.setStatus(Character.CharacterStatus.ACTIVE);
        character.setUpdatedAt(LocalDateTime.now());
        
        Character activatedCharacter = characterRepository.save(character);
        logger.info("Character activated successfully: {}", activatedCharacter.getId());
        
        return activatedCharacter;
    }
    
    /**
     * 停用角色
     */
    public Character deactivateCharacter(User user, Long characterId) {
        logger.info("Deactivating character ID: {} for user: {}", characterId, user.getUsername());
        
        Character character = getCharacterByIdAndUser(characterId, user);
        character.setStatus(Character.CharacterStatus.INACTIVE);
        character.setUpdatedAt(LocalDateTime.now());
        
        Character deactivatedCharacter = characterRepository.save(character);
        logger.info("Character deactivated successfully: {}", deactivatedCharacter.getId());
        
        return deactivatedCharacter;
    }
    
    /**
     * 获取用户的所有角色
     */
    @Transactional(readOnly = true)
    public Page<Character> getUserCharacters(User user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return characterRepository.findByUser(user, pageable);
    }
    
    /**
     * 获取用户的活跃角色
     */
    @Transactional(readOnly = true)
    public List<Character> getUserActiveCharacters(User user) {
        return characterRepository.findByUserAndStatus(user, Character.CharacterStatus.ACTIVE);
    }
    
    /**
     * 根据ID和用户获取角色
     */
    @Transactional(readOnly = true)
    public Character getCharacterByIdAndUser(Long characterId, User user) {
        return characterRepository.findByIdAndUser(characterId, user)
                .orElseThrow(() -> new IllegalArgumentException("角色不存在或无权限访问"));
    }
    
    /**
     * 根据知识库获取角色列表
     */
    @Transactional(readOnly = true)
    public List<Character> getCharactersByKnowledgeBase(User user, Long knowledgeBaseId) {
        return characterRepository.findByKnowledgeBaseIdAndUser(knowledgeBaseId, user);
    }
    
    /**
     * 搜索角色
     */
    @Transactional(readOnly = true)
    public List<Character> searchCharacters(User user, String keyword) {
        return characterRepository.searchByKeyword(user, keyword);
    }
    
    /**
     * 获取公开角色
     */
    @Transactional(readOnly = true)
    public Page<Character> getPublicCharacters(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return characterRepository.findPublicCharacters(Character.CharacterStatus.ACTIVE, pageable);
    }
    
    /**
     * 获取最近更新的角色
     */
    @Transactional(readOnly = true)
    public List<Character> getRecentlyUpdatedCharacters(User user, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return characterRepository.findRecentlyUpdated(user, pageable);
    }
    
    /**
     * 统计用户角色数量
     */
    @Transactional(readOnly = true)
    public long countUserCharacters(User user) {
        return characterRepository.countByUser(user);
    }
    
    /**
     * 统计用户活跃角色数量
     */
    @Transactional(readOnly = true)
    public long countUserActiveCharacters(User user) {
        return characterRepository.countByUserAndStatus(user, Character.CharacterStatus.ACTIVE);
    }
    
    /**
     * 检查角色名称是否可用
     */
    @Transactional(readOnly = true)
    public boolean isCharacterNameAvailable(User user, String name) {
        return !characterRepository.existsByUserAndName(user, name);
    }
    
    /**
     * 重新生成角色配置文件
     */
    public void regenerateCharacterProfile(User user, Long characterId) {
        logger.info("Regenerating profile for character ID: {} by user: {}", characterId, user.getUsername());
        
        Character character = getCharacterByIdAndUser(characterId, user);
        characterProfileService.regenerateProfile(character);
        
        logger.info("Profile regeneration initiated for character: {}", characterId);
    }
}