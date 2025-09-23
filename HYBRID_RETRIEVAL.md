# 混合检索功能说明

## 概述

混合检索（Hybrid Retrieval）结合了向量检索和关键词检索的优势，能够提供更准确和全面的检索结果。

## 功能特性

### 🔍 检索模式
- **向量检索**: 基于语义相似性的检索，理解问题含义
- **关键词检索**: 基于精确匹配的检索，支持全文搜索
- **混合融合**: 智能融合两种检索结果，提供最佳答案

### 🎯 重排序算法
- **相关性评分**: 基于关键词匹配和语义相似性
- **多样性优化**: 避免返回重复或相似的内容
- **智能排序**: 综合考虑相关性和多样性

## 配置说明

### 混合检索配置
```yaml
app:
  hybrid-retrieval:
    enabled: true              # 启用混合检索
    vector-weight: 0.7         # 向量检索权重
    keyword-weight: 0.3        # 关键词检索权重
    max-results: 10            # 最大检索结果数
```

### 重排序配置
```yaml
app:
  reranking:
    enabled: true              # 启用重排序
    max-results: 5             # 重排序后最大结果数
    diversity-weight: 0.1      # 多样性权重
    relevance-weight: 0.9      # 相关性权重
```

## 使用方法

### 1. 快速启用
```bash
# 运行设置脚本
./setup-hybrid-retrieval.sh
```

### 2. 手动配置
```bash
# 1. 创建数据库索引
psql -h localhost -U ragone_user -d ragone -f data/hybrid_retrieval_indexes.sql

# 2. 修改配置文件
# 编辑 src/main/resources/application.yml
# 将 hybrid-retrieval.enabled 设置为 true

# 3. 重启应用
./stop-dev.sh && ./start-dev.sh
```

## 技术实现

### 数据库索引
- **全文搜索索引**: 支持中文全文搜索
- **向量索引**: 优化向量相似性搜索
- **复合索引**: 提升混合检索性能

### 检索算法
1. **向量检索**: 使用余弦相似度计算语义相似性
2. **关键词检索**: 使用PostgreSQL全文搜索和模糊匹配
3. **结果融合**: 基于权重的分数融合算法
4. **重排序**: 考虑相关性和多样性的二次排序

## 性能优化

### 索引优化
- 为`content`字段创建全文搜索索引
- 为`embedding`字段创建向量索引
- 创建复合索引优化查询性能

### 查询优化
- 使用原生SQL避免ORM开销
- 限制检索结果数量
- 缓存常用查询结果

## 监控和调试

### 日志监控
```bash
# 查看检索日志
tail -f logs/ragone.log | grep "混合检索\|向量检索\|重排序"
```

### 性能指标
- 检索响应时间
- 检索结果数量
- 用户满意度

## 故障排除

### 常见问题

1. **索引创建失败**
   ```bash
   # 检查PostgreSQL连接
   psql -h localhost -U ragone_user -d ragone -c "SELECT 1;"
   
   # 检查pgvector扩展
   psql -h localhost -U ragone_user -d ragone -c "SELECT * FROM pg_extension WHERE extname = 'vector';"
   ```

2. **检索结果为空**
   - 检查知识库中是否有文档
   - 检查文档是否已处理完成
   - 检查embedding是否生成

3. **性能问题**
   - 检查数据库索引是否创建
   - 调整检索结果数量
   - 优化查询参数

### 回退方案
如果混合检索出现问题，系统会自动回退到纯向量检索：
```yaml
app:
  hybrid-retrieval:
    enabled: false  # 禁用混合检索，使用纯向量检索
```

## 最佳实践

### 权重调优
- **向量权重**: 适合语义搜索，理解问题含义
- **关键词权重**: 适合精确匹配，查找特定术语
- **建议比例**: 向量0.7，关键词0.3

### 结果数量
- **检索阶段**: 10-20个结果
- **重排序后**: 5-10个结果
- **最终输出**: 3-5个最相关片段

### 性能考虑
- 定期维护数据库索引
- 监控检索性能
- 根据使用情况调整参数

## 扩展功能

### 未来改进
- [ ] 支持多语言检索
- [ ] 添加查询扩展功能
- [ ] 实现自适应权重调整
- [ ] 添加检索结果缓存

### 自定义开发
可以参考现有代码实现自定义检索算法：
- `HybridRetrievalService`: 混合检索核心逻辑
- `RerankingService`: 重排序算法
- `DocumentChunkRepository`: 数据库查询方法
