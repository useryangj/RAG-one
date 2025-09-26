package com.example.ragone.service;

import com.example.ragone.entity.DocumentChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 重排序服务 - 对检索结果进行二次排序优化
 */
@Service
public class RerankingService {
    
    private static final Logger logger = LoggerFactory.getLogger(RerankingService.class);
    
    @Value("${app.reranking.enabled:false}")
    private boolean rerankingEnabled;
    
    @Value("${app.reranking.max-results:5}")
    private int maxRerankedResults;
    
    @Value("${app.reranking.diversity-weight:0.1}")
    private double diversityWeight;
    
    @Value("${app.reranking.relevance-weight:0.9}")
    private double relevanceWeight;
    
    /**
     * 对检索结果进行重排序
     */
    public List<DocumentChunk> rerankResults(List<DocumentChunk> chunks, String query) {
        if (!rerankingEnabled || chunks.isEmpty()) {
            return chunks.stream().limit(maxRerankedResults).collect(Collectors.toList());
        }
        
        try {
            // 1. 计算相关性分数
            Map<Long, Double> relevanceScores = calculateRelevanceScores(chunks, query);
            
            // 2. 计算多样性分数
            Map<Long, Double> diversityScores = calculateDiversityScores(chunks);
            
            // 3. 融合分数并排序
            List<DocumentChunk> rerankedChunks = chunks.stream()
                    .sorted((a, b) -> {
                        double scoreA = relevanceScores.getOrDefault(a.getId(), 0.0) * relevanceWeight +
                                       diversityScores.getOrDefault(a.getId(), 0.0) * diversityWeight;
                        double scoreB = relevanceScores.getOrDefault(b.getId(), 0.0) * relevanceWeight +
                                       diversityScores.getOrDefault(b.getId(), 0.0) * diversityWeight;
                        return Double.compare(scoreB, scoreA);
                    })
                    .limit(maxRerankedResults)
                    .collect(Collectors.toList());
            
            logger.debug("重排序完成 - 原始结果: {}, 重排序结果: {}", chunks.size(), rerankedChunks.size());
            
            return rerankedChunks;
            
        } catch (Exception e) {
            logger.error("重排序失败，返回原始结果", e);
            return chunks.stream().limit(maxRerankedResults).collect(Collectors.toList());
        }
    }
    
    /**
     * 计算相关性分数
     */
    private Map<Long, Double> calculateRelevanceScores(List<DocumentChunk> chunks, String query) {
        Map<Long, Double> scores = new HashMap<>();
        
        // 简单的关键词匹配分数计算
        String[] queryWords = query.toLowerCase().split("\\s+");
        
        for (DocumentChunk chunk : chunks) {
            double score = 0.0;
            String content = chunk.getContent().toLowerCase();
            
            // 计算关键词匹配分数
            for (String word : queryWords) {
                if (word.length() > 1) { // 忽略单字符
                    int count = countOccurrences(content, word);
                    score += count * (1.0 / word.length()); // 长词权重更高
                }
            }
            
            // 考虑文档长度（较短文档可能更相关）
            double lengthPenalty = Math.max(0.1, 1.0 - (content.length() / 10000.0));
            score *= lengthPenalty;
            
            scores.put(chunk.getId(), score);
        }
        
        return scores;
    }
    
    /**
     * 计算多样性分数 - 避免返回相似的内容
     */
    private Map<Long, Double> calculateDiversityScores(List<DocumentChunk> chunks) {
        Map<Long, Double> scores = new HashMap<>();
        
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk currentChunk = chunks.get(i);
            double diversityScore = 1.0;
            
            // 检查与前面片段的相似性
            for (int j = 0; j < i; j++) {
                DocumentChunk previousChunk = chunks.get(j);
                double similarity = calculateTextSimilarity(
                    currentChunk.getContent(), 
                    previousChunk.getContent()
                );
                diversityScore *= (1.0 - similarity);
            }
            
            scores.put(currentChunk.getId(), Math.max(0.0, diversityScore));
        }
        
        return scores;
    }
    
    /**
     * 计算文本相似性（简单的Jaccard相似性）
     */
    private double calculateTextSimilarity(String text1, String text2) {
        Set<String> words1 = Arrays.stream(text1.toLowerCase().split("\\s+"))
                .filter(word -> word.length() > 1)
                .collect(Collectors.toSet());
        
        Set<String> words2 = Arrays.stream(text2.toLowerCase().split("\\s+"))
                .filter(word -> word.length() > 1)
                .collect(Collectors.toSet());
        
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        
        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
    
    /**
     * 计算字符串中某个子串的出现次数
     */
    private int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }
    
    /**
     * 基于位置的简单重排序
     */
    public List<DocumentChunk> simpleRerank(List<DocumentChunk> chunks, String query) {
        if (chunks.isEmpty()) {
            return chunks;
        }
        
        // 简单的基于关键词匹配的重排序
        return chunks.stream()
                .sorted((a, b) -> {
                    double scoreA = calculateSimpleScore(a.getContent(), query);
                    double scoreB = calculateSimpleScore(b.getContent(), query);
                    return Double.compare(scoreB, scoreA);
                })
                .limit(maxRerankedResults)
                .collect(Collectors.toList());
    }
    
    /**
     * 计算简单的相关性分数
     */
    private double calculateSimpleScore(String content, String query) {
        String lowerContent = content.toLowerCase();
        String lowerQuery = query.toLowerCase();
        
        // 完全匹配得分最高
        if (lowerContent.contains(lowerQuery)) {
            return 1.0;
        }
        
        // 部分匹配
        String[] queryWords = lowerQuery.split("\\s+");
        double score = 0.0;
        for (String word : queryWords) {
            if (word.length() > 1 && lowerContent.contains(word)) {
                score += 0.5;
            }
        }
        
        return score / queryWords.length;
    }
    
    /**
     * 检查重排序是否启用
     */
    public boolean isRerankingEnabled() {
        return rerankingEnabled;
    }
}


