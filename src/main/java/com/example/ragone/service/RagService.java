package com.example.ragone.service;

import com.example.ragone.entity.DocumentChunk;
import com.example.ragone.entity.KnowledgeBase;
import com.example.ragone.entity.User;
import com.example.ragone.repository.DocumentChunkRepository;
import com.example.ragone.repository.KnowledgeBaseRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG问答服务
 */
@Service
public class RagService {
    
    private static final Logger logger = LoggerFactory.getLogger(RagService.class);
    
    @Autowired
    private ChatLanguageModel chatLanguageModel;
    
    @Autowired
    private EmbeddingModel embeddingModel;
    
    @Autowired
    private DocumentChunkRepository documentChunkRepository;
    
    @Autowired
    private KnowledgeBaseRepository knowledgeBaseRepository;
    
    /**
     * 基于知识库进行问答
     */
    public String askQuestion(String question, Long knowledgeBaseId, User user) {
        try {
            // 验证知识库权限
            KnowledgeBase knowledgeBase = knowledgeBaseRepository.findByIdAndUser(knowledgeBaseId, user)
                    .orElseThrow(() -> new RuntimeException("知识库不存在或无权访问"));
            
            // 1. 将问题转换为向量
            Embedding questionEmbedding = embeddingModel.embed(question).content();
            String embeddingString = embeddingToString(questionEmbedding);
            
            // 2. 在知识库中搜索相关文档片段
            List<DocumentChunk> relevantChunks = documentChunkRepository.findSimilarChunks(
                    knowledgeBaseId, embeddingString, 5);
            
            if (relevantChunks.isEmpty()) {
                return "抱歉，在您的知识库中没有找到相关信息。";
            }
            
            // 3. 构建上下文
            String context = relevantChunks.stream()
                    .map(DocumentChunk::getContent)
                    .collect(Collectors.joining("\n\n"));
            
            // 4. 构建提示词
            String prompt = buildPrompt(context, question);
            
            // 5. 调用大模型生成回答
            String response = chatLanguageModel.generate(prompt);
            
            logger.info("用户 {} 在知识库 {} 中提问: {}", user.getUsername(), knowledgeBase.getName(), question);
            
            return response;
            
        } catch (Exception e) {
            logger.error("RAG问答失败", e);
            return "抱歉，处理您的问题时出现了错误。请稍后再试。";
        }
    }
    
    /**
     * 构建提示词
     */
    private String buildPrompt(String context, String question) {
        return String.format("""
                请基于以下知识库内容回答用户的问题。如果知识库中没有相关信息，请明确说明。
                
                知识库内容：
                %s
                
                用户问题：%s
                
                请提供准确、有用的回答：
                """, context, question);
    }
    
    /**
     * 将Embedding转换为字符串格式（用于数据库存储）
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
}
