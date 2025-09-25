package com.example.ragone.repository;

import com.example.ragone.entity.Character;
import com.example.ragone.entity.RolePlaySession;
import com.example.ragone.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 角色扮演会话Repository接口
 */
@Repository
public interface RolePlaySessionRepository extends JpaRepository<RolePlaySession, String> {
    
    /**
     * 根据用户查找所有会话（分页）
     */
    Page<RolePlaySession> findByUserOrderByLastActivityAtDesc(User user, Pageable pageable);
    
    /**
     * 根据用户和角色查找会话
     */
    List<RolePlaySession> findByUserAndCharacterOrderByLastActivityAtDesc(User user, Character character);
    
    /**
     * 根据用户ID和会话ID查找会话
     */
    Optional<RolePlaySession> findBySessionIdAndUser(String sessionId, User user);
    
    /**
     * 根据状态查找会话
     */
    List<RolePlaySession> findByStatus(RolePlaySession.SessionStatus status);
    
    /**
     * 根据用户和状态查找会话
     */
    List<RolePlaySession> findByUserAndStatus(User user, RolePlaySession.SessionStatus status);
    
    /**
     * 查找用户的活跃会话
     */
    @Query("SELECT rps FROM RolePlaySession rps WHERE rps.user = :user AND rps.status = 'ACTIVE' ORDER BY rps.lastActivityAt DESC")
    List<RolePlaySession> findActiveSessionsByUser(@Param("user") User user);
    
    /**
     * 查找指定时间之前的非活跃会话
     */
    @Query("SELECT rps FROM RolePlaySession rps WHERE rps.lastActivityAt < :cutoffTime AND rps.status = 'ACTIVE'")
    List<RolePlaySession> findInactiveSessionsBefore(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * 统计用户的会话数量
     */
    long countByUser(User user);
    
    /**
     * 统计用户在指定状态下的会话数量
     */
    long countByUserAndStatus(User user, RolePlaySession.SessionStatus status);
    
    /**
     * 统计角色的会话数量
     */
    long countByCharacter(Character character);
    
    /**
     * 查找最近活跃的会话
     */
    @Query("SELECT rps FROM RolePlaySession rps WHERE rps.user = :user ORDER BY rps.lastActivityAt DESC")
    List<RolePlaySession> findRecentSessions(@Param("user") User user, Pageable pageable);
    
    /**
     * 根据会话名称模糊搜索
     */
    @Query("SELECT rps FROM RolePlaySession rps WHERE rps.user = :user AND rps.sessionName LIKE CONCAT('%', :keyword, '%') ORDER BY rps.lastActivityAt DESC")
    List<RolePlaySession> searchBySessionName(@Param("user") User user, @Param("keyword") String keyword);
    
    /**
     * 查找指定角色的所有会话
     */
    @Query("SELECT rps FROM RolePlaySession rps WHERE rps.character = :character ORDER BY rps.createdAt DESC")
    List<RolePlaySession> findByCharacterOrderByCreatedAtDesc(@Param("character") Character character);
}