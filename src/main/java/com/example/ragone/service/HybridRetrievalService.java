package com.example.ragone.service;

import com.example.ragone.entity.DocumentChunk;
import com.example.ragone.repository.DocumentChunkRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 混合检索服务 - 结合关键词检索和向量检索
 */
@Service
public class HybridRetrievalService {
    
    private static final Logger logger = LoggerFactory.getLogger(HybridRetrievalService.class);
    
    @Autowired
    private DocumentChunkRepository documentChunkRepository;
    
    @Autowired
    private EmbeddingModel embeddingModel;
    
    @Value("${app.hybrid-retrieval.vector-weight:0.7}")
    private double vectorWeight;
    
    @Value("${app.hybrid-retrieval.keyword-weight:0.3}")
    private double keywordWeight;
    
    @Value("${app.hybrid-retrieval.max-results:10}")
    private int maxResults;
    
    @Value("${app.hybrid-retrieval.enabled:false}")
    private boolean hybridEnabled;
    
    /**
     * 混合检索 - 结合向量检索和关键词检索
     */
    public List<DocumentChunk> hybridSearch(String query, Long knowledgeBaseId) {
        if (!hybridEnabled) {
            // 如果混合检索未启用，回退到纯向量检索
            return vectorSearch(query, knowledgeBaseId);
        }
        
        try {
            // 1. 向量检索
            List<DocumentChunk> vectorResults = vectorSearch(query, knowledgeBaseId);
            
            // 2. 关键词检索
            List<DocumentChunk> keywordResults = keywordSearch(query, knowledgeBaseId);
            
            // 3. 结果融合和重排序
            List<DocumentChunk> hybridResults = fuseAndRerankResults(
                vectorResults, keywordResults, query, knowledgeBaseId);
            
            logger.info("混合检索完成 - 向量结果: {}, 关键词结果: {}, 融合结果: {}", 
                vectorResults.size(), keywordResults.size(), hybridResults.size());
            
            return hybridResults;
            
        } catch (Exception e) {
            logger.error("混合检索失败，回退到向量检索", e);
            return vectorSearch(query, knowledgeBaseId);
        }
    }
    
    /**
     * 向量检索
     */
    private List<DocumentChunk> vectorSearch(String query, Long knowledgeBaseId) {
        try {
            Embedding queryEmbedding = embeddingModel.embed(query).content();
            String embeddingString = embeddingToString(queryEmbedding);
            
            return documentChunkRepository.findSimilarChunks(
                knowledgeBaseId, embeddingString, maxResults);
                
        } catch (Exception e) {
            logger.error("向量检索失败", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 关键词检索 - 使用PostgreSQL全文搜索
     */
    private List<DocumentChunk> keywordSearch(String query, Long knowledgeBaseId) {
        try {
            // 使用PostgreSQL的全文搜索功能
            return documentChunkRepository.findByKeywordSearch(knowledgeBaseId, query, maxResults);
            
        } catch (Exception e) {
            logger.error("关键词检索失败", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 结果融合和重排序
     */
    private List<DocumentChunk> fuseAndRerankResults(List<DocumentChunk> vectorResults, 
                                                   List<DocumentChunk> keywordResults, 
                                                   String query, Long knowledgeBaseId) {
        
        // 创建文档ID到分数的映射
        Map<Long, Double> docScores = new HashMap<>();
        
        // 处理向量检索结果
        for (int i = 0; i < vectorResults.size(); i++) {
            DocumentChunk chunk = vectorResults.get(i);
            // 向量检索分数：位置越靠前分数越高
            double vectorScore = 1.0 - (double) i / vectorResults.size();
            docScores.merge(chunk.getId(), vectorScore * vectorWeight, Double::sum);
        }
        
        // 处理关键词检索结果
        for (int i = 0; i < keywordResults.size(); i++) {
            DocumentChunk chunk = keywordResults.get(i);
            // 关键词检索分数：位置越靠前分数越高
            double keywordScore = 1.0 - (double) i / keywordResults.size();
            docScores.merge(chunk.getId(), keywordScore * keywordWeight, Double::sum);
        }
        
        // 合并所有结果并去重
        Set<DocumentChunk> allChunks = new HashSet<>();
        allChunks.addAll(vectorResults);
        allChunks.addAll(keywordResults);
        
        // 按融合分数排序
        return allChunks.stream()
                .sorted((a, b) -> Double.compare(
                    docScores.getOrDefault(b.getId(), 0.0),
                    docScores.getOrDefault(a.getId(), 0.0)))
                .limit(maxResults)
                .collect(Collectors.toList());
    }
    
    /**
     * 将Embedding转换为字符串
     */
    private String embeddingToString(Embedding embedding) {
        float[] vector = embedding.vector();
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * 检查混合检索是否启用
     */
    public boolean isHybridEnabled() {
        return hybridEnabled;
    }
}
