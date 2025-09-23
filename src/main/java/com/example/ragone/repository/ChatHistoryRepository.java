package com.example.ragone.repository;

import com.example.ragone.entity.ChatHistory;
import com.example.ragone.entity.KnowledgeBase;
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
 * 聊天历史Repository
 */
@Repository
public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Long> {
    
    /**
     * 根据会话ID查找聊天历史
     */
    List<ChatHistory> findBySessionIdOrderByCreatedAtAsc(String sessionId);
    
    /**
     * 根据用户ID查找聊天历史（分页）
     */
    @Query("SELECT ch FROM ChatHistory ch WHERE ch.user.id = :userId ORDER BY ch.createdAt DESC")
    Page<ChatHistory> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * 根据用户ID和知识库ID查找聊天历史
     */
    @Query("SELECT ch FROM ChatHistory ch WHERE ch.user.id = :userId AND ch.knowledgeBase.id = :knowledgeBaseId ORDER BY ch.createdAt DESC")
    List<ChatHistory> findByUserIdAndKnowledgeBaseIdOrderByCreatedAtDesc(@Param("userId") Long userId, @Param("knowledgeBaseId") Long knowledgeBaseId);
    
    /**
     * 根据会话ID查找聊天历史（分页）
     */
    Page<ChatHistory> findBySessionIdOrderByCreatedAtDesc(String sessionId, Pageable pageable);
    
    /**
     * 根据用户ID和会话ID查找聊天历史
     */
    @Query("SELECT ch FROM ChatHistory ch WHERE ch.user.id = :userId AND ch.sessionId = :sessionId ORDER BY ch.createdAt ASC")
    List<ChatHistory> findByUserIdAndSessionIdOrderByCreatedAtAsc(@Param("userId") Long userId, @Param("sessionId") String sessionId);
    
    /**
     * 统计用户的聊天历史数量
     */
    @Query("SELECT COUNT(ch) FROM ChatHistory ch WHERE ch.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);
    
    /**
     * 统计会话的聊天历史数量
     */
    long countBySessionId(String sessionId);
    
    /**
     * 查找指定时间范围内的聊天历史
     */
    @Query("SELECT ch FROM ChatHistory ch WHERE ch.user.id = :userId AND ch.createdAt BETWEEN :startTime AND :endTime ORDER BY ch.createdAt DESC")
    List<ChatHistory> findByUserIdAndCreatedAtBetween(@Param("userId") Long userId, 
                                                     @Param("startTime") LocalDateTime startTime, 
                                                     @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查找用户的所有会话ID
     */
    @Query("SELECT DISTINCT ch.sessionId FROM ChatHistory ch WHERE ch.user.id = :userId ORDER BY ch.sessionId")
    List<String> findDistinctSessionIdsByUserId(@Param("userId") Long userId);
    
    /**
     * 删除指定会话的所有聊天历史
     */
    void deleteBySessionId(String sessionId);
    
    /**
     * 删除指定时间之前的聊天历史
     */
    void deleteByCreatedAtBefore(LocalDateTime cutoffTime);
    
    /**
     * 查找用户最近的会话
     */
    @Query("SELECT ch FROM ChatHistory ch WHERE ch.user.id = :userId AND ch.sessionId = :sessionId ORDER BY ch.createdAt DESC")
    List<ChatHistory> findByUserIdAndSessionIdOrderByCreatedAtDesc(@Param("userId") Long userId,
                                                    @Param("sessionId") String sessionId, 
                                                    Pageable pageable);
}

