package com.example.ragone.repository;

import com.example.ragone.entity.Character;
import com.example.ragone.entity.CharacterProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 角色配置文件Repository接口
 */
@Repository
public interface CharacterProfileRepository extends JpaRepository<CharacterProfile, Long> {
    
    /**
     * 根据角色查找配置文件
     */
    Optional<CharacterProfile> findByCharacter(Character character);
    
    /**
     * 根据角色ID查找配置文件
     */
    @Query("SELECT cp FROM CharacterProfile cp WHERE cp.character.id = :characterId")
    Optional<CharacterProfile> findByCharacterId(@Param("characterId") Long characterId);
    
    /**
     * 根据状态查找配置文件
     */
    List<CharacterProfile> findByStatus(CharacterProfile.ProfileStatus status);
    
    /**
     * 根据生成方法查找配置文件
     */
    List<CharacterProfile> findByGenerationMethod(CharacterProfile.GenerationMethod generationMethod);
    
    /**
     * 查找指定角色的最新版本配置文件
     */
    @Query("SELECT cp FROM CharacterProfile cp WHERE cp.character = :character ORDER BY cp.version DESC")
    List<CharacterProfile> findByCharacterOrderByVersionDesc(@Param("character") Character character);
    
    /**
     * 查找指定角色的指定版本配置文件
     */
    @Query("SELECT cp FROM CharacterProfile cp WHERE cp.character = :character AND cp.version = :version")
    Optional<CharacterProfile> findByCharacterAndVersion(@Param("character") Character character, @Param("version") Integer version);
    
    /**
     * 检查角色是否已有配置文件
     */
    boolean existsByCharacter(Character character);
    
    /**
     * 统计指定状态的配置文件数量
     */
    long countByStatus(CharacterProfile.ProfileStatus status);
    
    /**
     * 查找需要重新生成的配置文件（状态为FAILED或DRAFT）
     */
    @Query("SELECT cp FROM CharacterProfile cp WHERE cp.status IN ('FAILED', 'DRAFT') ORDER BY cp.updatedAt ASC")
    List<CharacterProfile> findProfilesNeedingRegeneration();
    
    /**
     * 查找正在生成中的配置文件
     */
    List<CharacterProfile> findByStatusOrderByUpdatedAtAsc(CharacterProfile.ProfileStatus status);
}