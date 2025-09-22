package com.example.ragone.repository;

import com.example.ragone.entity.Document;
import com.example.ragone.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 文档片段Repository接口
 */
@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {
    
    /**
     * 根据文档查找所有片段
     */
    List<DocumentChunk> findByDocumentOrderByChunkPosition(Document document);
    
    /**
     * 根据知识库ID进行向量相似性搜索（优化版本 - 无需JOIN）
     * 直接使用knowledge_base_id字段，避免JOIN操作
     */
    @Query(value = """
        SELECT * FROM document_chunks
        WHERE knowledge_base_id = :knowledgeBaseId
        AND embedding IS NOT NULL
        ORDER BY embedding <=> CAST(:queryEmbedding AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<DocumentChunk> findSimilarChunks(@Param("knowledgeBaseId") Long knowledgeBaseId,
                                         @Param("queryEmbedding") String queryEmbedding,
                                         @Param("limit") int limit);
    
    /**
     * 旧版本查询方法（保留作为备用）
     */
    @Query(value = """
        SELECT dc.* FROM document_chunks dc
        JOIN documents d ON dc.document_id = d.id
        WHERE d.knowledge_base_id = :knowledgeBaseId
        AND dc.embedding IS NOT NULL
        ORDER BY dc.embedding <=> CAST(:queryEmbedding AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<DocumentChunk> findSimilarChunksWithJoin(@Param("knowledgeBaseId") Long knowledgeBaseId,
                                                  @Param("queryEmbedding") String queryEmbedding,
                                                  @Param("limit") int limit);
    
    /**
     * 根据文档删除所有片段
     */
    void deleteByDocument(Document document);
    
    /**
     * 统计文档的片段数量
     */
    int countByDocument(Document document);
    
    /**
     * 使用原生SQL保存文档片段（处理vector类型，包含knowledge_base_id）
     */
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO document_chunks 
        (document_id, knowledge_base_id, chunk_position, content, content_hash, token_count, embedding, created_at)
        VALUES (:documentId, :knowledgeBaseId, :chunkPosition, :content, :contentHash, :tokenCount, 
                CAST(:embedding AS vector), :createdAt)
        """, nativeQuery = true)
    void saveChunkWithVector(@Param("documentId") Long documentId,
                           @Param("knowledgeBaseId") Long knowledgeBaseId,
                           @Param("chunkPosition") Integer chunkPosition,
                           @Param("content") String content,
                           @Param("contentHash") String contentHash,
                           @Param("tokenCount") Integer tokenCount,
                           @Param("embedding") String embedding,
                           @Param("createdAt") java.time.LocalDateTime createdAt);
}
