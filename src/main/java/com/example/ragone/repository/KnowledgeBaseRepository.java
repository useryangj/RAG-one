package com.example.ragone.repository;

import com.example.ragone.entity.KnowledgeBase;
import com.example.ragone.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 知识库Repository接口
 */
@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {
    
    /**
     * 根据用户查找所有知识库
     */
    List<KnowledgeBase> findByUserAndActiveTrue(User user);
    
    /**
     * 根据用户和知识库名称查找知识库
     */
    Optional<KnowledgeBase> findByUserAndName(User user, String name);
    
    /**
     * 检查用户是否已有同名知识库
     */
    boolean existsByUserAndName(User user, String name);
    
    /**
     * 根据用户ID和知识库ID查找知识库
     */
    Optional<KnowledgeBase> findByIdAndUser(Long id, User user);
}
