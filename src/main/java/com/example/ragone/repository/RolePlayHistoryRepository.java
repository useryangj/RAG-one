package com.example.ragone.repository;

import com.example.ragone.entity.Character;
import com.example.ragone.entity.RolePlayHistory;
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

/**
 * 角色扮演对话历史Repository接口
 */
@Repository
public interface RolePlayHistoryRepository extends JpaRepository<RolePlayHistory, Long> {
    
    /**
     * 根据会话查找对话历史（按轮次排序）
     */
    List<RolePlayHistory> findByRolePlaySessionOrderByTurnNumberAsc(RolePlaySession rolePlaySession);
    
    /**
     * 根据会话查找对话历史（分页）
     */
    Page<RolePlayHistory> findByRolePlaySessionOrderByTurnNumberAsc(RolePlaySession rolePlaySession, Pageable pageable);
    
    /**
     * 根据用户查找对话历史
     */
    Page<RolePlayHistory> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    
    /**
     * 根据角色查找对话历史
     */
    Page<RolePlayHistory> findByCharacterOrderByCreatedAtDesc(Character character, Pageable pageable);
    
    /**
     * 查找会话的最新N条对话记录
     */
    @Query("SELECT rph FROM RolePlayHistory rph WHERE rph.rolePlaySession = :session ORDER BY rph.turnNumber DESC")
    List<RolePlayHistory> findRecentHistoryBySession(@Param("session") RolePlaySession session, Pageable pageable);
    
    /**
     * 查找会话的指定轮次范围内的对话记录
     */
    @Query("SELECT rph FROM RolePlayHistory rph WHERE rph.rolePlaySession = :session AND rph.turnNumber BETWEEN :startTurn AND :endTurn ORDER BY rph.turnNumber ASC")
    List<RolePlayHistory> findHistoryByTurnRange(@Param("session") RolePlaySession session, @Param("startTurn") Integer startTurn, @Param("endTurn") Integer endTurn);
    
    /**
     * 统计会话的对话轮次数
     */
    @Query("SELECT COUNT(rph) FROM RolePlayHistory rph WHERE rph.rolePlaySession = :session")
    long countByRolePlaySession(@Param("session") RolePlaySession session);
    
    /**
     * 查找会话的最大轮次号
     */
    @Query("SELECT MAX(rph.turnNumber) FROM RolePlayHistory rph WHERE rph.rolePlaySession = :session")
    Integer findMaxTurnNumberBySession(@Param("session") RolePlaySession session);
    
    /**
     * 根据用户消息内容搜索对话历史
     */
    @Query("SELECT rph FROM RolePlayHistory rph WHERE rph.user = :user AND (rph.userMessage LIKE %:keyword% OR rph.characterResponse LIKE %:keyword%) ORDER BY rph.createdAt DESC")
    List<RolePlayHistory> searchByMessageContent(@Param("user") User user, @Param("keyword") String keyword);
    
    /**
     * 查找指定时间范围内的对话历史
     */
    @Query("SELECT rph FROM RolePlayHistory rph WHERE rph.createdAt BETWEEN :startTime AND :endTime ORDER BY rph.createdAt DESC")
    List<RolePlayHistory> findByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查找使用了RAG检索的对话记录
     */
    List<RolePlayHistory> findByUsedRagTrueOrderByCreatedAtDesc();
    
    /**
     * 查找有用户评分的对话记录
     */
    @Query("SELECT rph FROM RolePlayHistory rph WHERE rph.userRating IS NOT NULL ORDER BY rph.createdAt DESC")
    List<RolePlayHistory> findRatedHistories();
    
    /**
     * 统计用户的对话总数
     */
    long countByUser(User user);
    
    /**
     * 统计角色的对话总数
     */
    long countByCharacter(Character character);
    
    /**
     * 查找指定会话的最后一条对话记录
     */
    @Query("SELECT rph FROM RolePlayHistory rph WHERE rph.rolePlaySession = :session ORDER BY rph.turnNumber DESC")
    List<RolePlayHistory> findLastHistoryBySession(@Param("session") RolePlaySession session, Pageable pageable);
    
    /**
     * 删除指定会话的所有对话历史
     */
    void deleteByRolePlaySession(RolePlaySession rolePlaySession);
    
    /**
     * 删除指定时间之前的对话历史
     */
    @Query("DELETE FROM RolePlayHistory rph WHERE rph.createdAt < :cutoffTime")
    void deleteHistoriesBefore(@Param("cutoffTime") LocalDateTime cutoffTime);
}