package com.example.ragone.repository;

import com.example.ragone.entity.Character;
import com.example.ragone.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 角色Repository接口
 */
@Repository
public interface CharacterRepository extends JpaRepository<Character, Long> {
    
    /**
     * 根据用户查找所有活跃角色
     */
    List<Character> findByUserAndStatus(User user, Character.CharacterStatus status);
    
    /**
     * 根据用户查找所有角色（分页）
     */
    Page<Character> findByUser(User user, Pageable pageable);
    
    /**
     * 根据用户和角色名称查找角色
     */
    Optional<Character> findByUserAndName(User user, String name);
    
    /**
     * 检查用户是否已有同名角色
     */
    boolean existsByUserAndName(User user, String name);
    
    /**
     * 根据用户ID和角色ID查找角色
     */
    Optional<Character> findByIdAndUser(Long id, User user);
    
    /**
     * 根据知识库ID查找所有角色
     */
    @Query("SELECT c FROM Character c WHERE c.knowledgeBase.id = :knowledgeBaseId AND c.user = :user")
    List<Character> findByKnowledgeBaseIdAndUser(@Param("knowledgeBaseId") Long knowledgeBaseId, @Param("user") User user);
    
    /**
     * 查找公开的角色（可供其他用户使用）
     */
    @Query("SELECT c FROM Character c WHERE c.isPublic = true AND c.status = :status ORDER BY c.createdAt DESC")
    Page<Character> findPublicCharacters(@Param("status") Character.CharacterStatus status, Pageable pageable);
    
    /**
     * 根据角色名称模糊搜索（用户自己的角色）
     */
    @Query("SELECT c FROM Character c WHERE c.user = :user AND (c.name LIKE %:keyword% OR c.description LIKE %:keyword%) ORDER BY c.updatedAt DESC")
    List<Character> searchByKeyword(@Param("user") User user, @Param("keyword") String keyword);
    
    /**
     * 统计用户的角色数量
     */
    long countByUser(User user);
    
    /**
     * 统计用户在指定状态下的角色数量
     */
    long countByUserAndStatus(User user, Character.CharacterStatus status);
    
    /**
     * 查找最近更新的角色
     */
    @Query("SELECT c FROM Character c WHERE c.user = :user ORDER BY c.updatedAt DESC")
    List<Character> findRecentlyUpdated(@Param("user") User user, Pageable pageable);
}