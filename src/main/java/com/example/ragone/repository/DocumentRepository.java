package com.example.ragone.repository;

import com.example.ragone.entity.Document;
import com.example.ragone.entity.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 文档Repository接口
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    
    /**
     * 根据知识库查找所有文档
     */
    List<Document> findByKnowledgeBase(KnowledgeBase knowledgeBase);
    
    /**
     * 根据知识库和处理状态查找文档
     */
    List<Document> findByKnowledgeBaseAndProcessStatus(KnowledgeBase knowledgeBase, Document.ProcessStatus status);
    
    /**
     * 根据文件哈希查找文档
     */
    Optional<Document> findByFileHash(String fileHash);
    
    /**
     * 根据知识库和文档ID查找文档
     */
    Optional<Document> findByIdAndKnowledgeBase(Long id, KnowledgeBase knowledgeBase);
}
