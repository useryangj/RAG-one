package com.example.ragone.service;

import com.example.ragone.entity.Document;
import com.example.ragone.entity.KnowledgeBase;
import com.example.ragone.entity.User;
import com.example.ragone.repository.DocumentChunkRepository;
import com.example.ragone.repository.DocumentRepository;
import com.example.ragone.repository.KnowledgeBaseRepository;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 文档处理服务
 */
@Service
public class DocumentService {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);
    
    @Autowired
    private DocumentRepository documentRepository;
    
    @Autowired
    private DocumentChunkRepository documentChunkRepository;
    
    @Autowired
    private KnowledgeBaseRepository knowledgeBaseRepository;
    
    @Autowired
    private EmbeddingModel embeddingModel;
    
    @Value("${app.file-storage-path}")
    private String fileStoragePath;
    
    @Value("${app.document.chunk.max-chars:400}")
    private int maxChunkChars;
    
    @Value("${app.document.chunk.overlap-chars:50}")
    private int overlapChars;
    
    @Value("${app.document.chunk.max-tokens:500}")
    private int maxTokens;
    
    private final DocumentParser documentParser = new ApacheTikaDocumentParser();
    
    /**
     * 上传并处理文档
     */
    @Transactional
    public Document uploadDocument(MultipartFile file, Long knowledgeBaseId, User user) throws IOException {
        // 验证知识库权限
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findByIdAndUser(knowledgeBaseId, user)
                .orElseThrow(() -> new RuntimeException("知识库不存在或无权访问"));
        
        // 创建存储目录
        Path storageDir = Paths.get(fileStoragePath, "users", user.getId().toString(), "kb", knowledgeBaseId.toString());
        Files.createDirectories(storageDir);
        
        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
        Path filePath = storageDir.resolve(uniqueFilename);
        
        // 保存文件
        Files.copy(file.getInputStream(), filePath);
        
        // 计算文件哈希
        String fileHash = calculateFileHash(file.getBytes());
        
        // 检查是否已存在相同文件
        if (documentRepository.findByFileHash(fileHash).isPresent()) {
            Files.deleteIfExists(filePath);
            throw new RuntimeException("文件已存在");
        }
        
        // 创建文档记录
        Document document = new Document();
        document.setFilename(uniqueFilename);
        document.setOriginalFilename(originalFilename);
        document.setFilePath(filePath.toString());
        document.setFileSize(file.getSize());
        document.setMimeType(file.getContentType());
        document.setFileHash(fileHash);
        document.setKnowledgeBase(knowledgeBase);
        document.setProcessStatus(Document.ProcessStatus.PENDING);
        
        document = documentRepository.save(document);
        
        // 异步处理文档（这里简化为同步处理）
        processDocumentAsync(document);
        
        return document;
    }
    
    /**
     * 处理文档（解析、分块、向量化）
     */
    @Transactional
    public void processDocumentAsync(Document document) {
        try {
            document.setProcessStatus(Document.ProcessStatus.PROCESSING);
            documentRepository.save(document);
            
            // 1. 解析文档内容
            dev.langchain4j.data.document.Document langchainDoc = documentParser.parse(
                    Files.newInputStream(Paths.get(document.getFilePath()))
            );
            
            // 2. 文档分块 - 使用配置参数
            List<TextSegment> segments = DocumentSplitters.recursive(
                    maxChunkChars,  // 每块最大字符数
                    overlapChars    // 重叠字符数
            ).split(langchainDoc);
            
            // 3. 为每个分块生成向量并保存
            int position = 0;
            for (TextSegment segment : segments) {
                try {
                    String text = segment.text();
                    
                    // 检查文本长度，如果过长则跳过
                    int estimatedTokens = estimateTokenCount(text);
                    if (estimatedTokens > maxTokens) {
                        logger.warn("跳过过长的文档片段，估计token数: {}, 文本长度: {}, 限制: {}", estimatedTokens, text.length(), maxTokens);
                        continue;
                    }
                    
                    // 生成向量
                    Embedding embedding = embeddingModel.embed(text).content();
                    String embeddingString = embeddingToVectorString(embedding);
                    
                    // 使用原生SQL保存文档片段（处理vector类型，包含knowledge_base_id）
                    documentChunkRepository.saveChunkWithVector(
                        document.getId(),
                        document.getKnowledgeBase().getId(), // 冗余存储knowledge_base_id
                        position++,
                        text,
                        calculateContentHash(text),
                        estimatedTokens, // 使用更准确的token估算
                        embeddingString,
                        LocalDateTime.now()
                    );
                    
                } catch (Exception e) {
                    logger.error("处理文档片段失败: {}", e.getMessage());
                    // 如果是token限制错误，记录更详细的信息
                    if (e.getMessage() != null && e.getMessage().contains("512 tokens")) {
                        logger.error("文档片段超过512 token限制，文本长度: {}", segment.text().length());
                    }
                }
            }
            
            // 4. 更新文档状态
            document.setProcessStatus(Document.ProcessStatus.COMPLETED);
            document.setChunkCount(position);
            document.setProcessedAt(LocalDateTime.now());
            document.setProcessMessage("处理完成");
            documentRepository.save(document);
            
            logger.info("文档处理完成: {}, 共生成 {} 个片段", document.getOriginalFilename(), position);
            
        } catch (Exception e) {
            logger.error("文档处理失败", e);
            document.setProcessStatus(Document.ProcessStatus.FAILED);
            document.setProcessMessage(e.getMessage());
            documentRepository.save(document);
        }
    }
    
    /**
     * 删除文档
     */
    @Transactional
    public void deleteDocument(Long documentId, User user) throws IOException {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("文档不存在"));
        
        // 验证权限
        if (!document.getKnowledgeBase().getUser().getId().equals(user.getId())) {
            throw new RuntimeException("无权删除此文档");
        }
        
        // 删除文件
        Files.deleteIfExists(Paths.get(document.getFilePath()));
        
        // 删除数据库记录（级联删除文档片段）
        documentRepository.delete(document);
    }
    
    /**
     * 获取用户的文档列表
     */
    public List<Document> getUserDocuments(Long knowledgeBaseId, User user) {
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findByIdAndUser(knowledgeBaseId, user)
                .orElseThrow(() -> new RuntimeException("知识库不存在或无权访问"));
        
        return documentRepository.findByKnowledgeBase(knowledgeBase);
    }
    
    /**
     * 计算文件哈希
     */
    private String calculateFileHash(byte[] content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(content);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("计算文件哈希失败", e);
        }
    }
    
    /**
     * 计算内容哈希
     */
    private String calculateContentHash(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(content.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("计算内容哈希失败", e);
        }
    }
    
    /**
     * 估算文本的token数量
     * 对于中文文本，通常1个字符约等于1-2个token
     * 对于英文文本，通常1个单词约等于1个token
     */
    private int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        // 简单估算：中文字符按1.5个token计算，英文单词按1个token计算
        int chineseChars = 0;
        int englishWords = 0;
        
        for (char c : text.toCharArray()) {
            if (c >= 0x4E00 && c <= 0x9FFF) { // 中文字符范围
                chineseChars++;
            } else if (Character.isLetter(c)) {
                // 简单计算英文单词（按空格分割）
                if (c == ' ') {
                    englishWords++;
                }
            }
        }
        
        // 估算token数：中文字符 * 1.5 + 英文单词数
        int estimatedTokens = (int) (chineseChars * 1.5) + englishWords + 1;
        
        // 如果估算结果太小，使用字符数/2作为备选
        int fallbackEstimate = text.length() / 2;
        
        return Math.max(estimatedTokens, fallbackEstimate);
    }
    
    /**
     * 将Embedding转换为PostgreSQL vector格式
     */
    private String embeddingToVectorString(Embedding embedding) {
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
