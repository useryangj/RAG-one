package com.example.ragone.service;

import com.example.ragone.entity.KnowledgeBase;
import com.example.ragone.entity.User;
import com.example.ragone.repository.KnowledgeBaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 知识库服务
 */
@Service
public class KnowledgeBaseService {
    
    @Autowired
    private KnowledgeBaseRepository knowledgeBaseRepository;
    
    /**
     * 创建知识库
     */
    @Transactional
    public KnowledgeBase createKnowledgeBase(String name, String description, User user) {
        // 检查是否已存在同名知识库
        if (knowledgeBaseRepository.existsByUserAndName(user, name)) {
            throw new RuntimeException("知识库名称已存在");
        }
        
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setName(name);
        knowledgeBase.setDescription(description);
        knowledgeBase.setUser(user);
        knowledgeBase.setActive(true);
        
        return knowledgeBaseRepository.save(knowledgeBase);
    }
    
    /**
     * 获取用户的所有知识库
     */
    public List<KnowledgeBase> getUserKnowledgeBases(User user) {
        return knowledgeBaseRepository.findByUserAndActiveTrue(user);
    }
    
    /**
     * 更新知识库
     */
    @Transactional
    public KnowledgeBase updateKnowledgeBase(Long id, String name, String description, User user) {
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("知识库不存在或无权访问"));
        
        // 如果名称发生变化，检查是否与其他知识库重名
        if (!knowledgeBase.getName().equals(name)) {
            if (knowledgeBaseRepository.existsByUserAndName(user, name)) {
                throw new RuntimeException("知识库名称已存在");
            }
        }
        
        knowledgeBase.setName(name);
        knowledgeBase.setDescription(description);
        
        return knowledgeBaseRepository.save(knowledgeBase);
    }
    
    /**
     * 删除知识库
     */
    @Transactional
    public void deleteKnowledgeBase(Long id, User user) {
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("知识库不存在或无权访问"));
        
        // 软删除
        knowledgeBase.setActive(false);
        knowledgeBaseRepository.save(knowledgeBase);
    }
    
    /**
     * 获取知识库详情
     */
    public KnowledgeBase getKnowledgeBase(Long id, User user) {
        return knowledgeBaseRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("知识库不存在或无权访问"));
    }
}
