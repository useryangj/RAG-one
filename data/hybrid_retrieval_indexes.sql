-- 混合检索优化索引脚本
-- 为支持关键词检索和向量检索创建必要的索引

-- 1. 为document_chunks表的content字段创建全文搜索索引
-- 支持中文全文搜索
CREATE INDEX IF NOT EXISTS idx_document_chunks_content_fts 
ON document_chunks 
USING gin(to_tsvector('chinese', content));

-- 2. 为knowledge_base_id创建复合索引，优化混合检索查询
CREATE INDEX IF NOT EXISTS idx_document_chunks_kb_content 
ON document_chunks (knowledge_base_id, content);

-- 3. 为embedding字段创建向量索引（如果还没有）
-- 注意：这需要pgvector扩展
CREATE INDEX IF NOT EXISTS idx_document_chunks_embedding_cosine 
ON document_chunks 
USING ivfflat (embedding vector_cosine_ops) 
WITH (lists = 100);

-- 4. 为content字段创建B-tree索引，支持LIKE查询
CREATE INDEX IF NOT EXISTS idx_document_chunks_content_btree 
ON document_chunks (content);

-- 5. 创建复合索引，同时支持向量和关键词检索
CREATE INDEX IF NOT EXISTS idx_document_chunks_kb_embedding 
ON document_chunks (knowledge_base_id) 
WHERE embedding IS NOT NULL;

-- 6. 为token_count创建索引，用于结果排序优化
CREATE INDEX IF NOT EXISTS idx_document_chunks_token_count 
ON document_chunks (token_count);

-- 7. 创建部分索引，只对非空embedding创建向量索引
CREATE INDEX IF NOT EXISTS idx_document_chunks_embedding_partial 
ON document_chunks 
USING ivfflat (embedding vector_cosine_ops) 
WITH (lists = 100)
WHERE embedding IS NOT NULL;

-- 8. 为chunk_position创建索引，支持按位置排序
CREATE INDEX IF NOT EXISTS idx_document_chunks_position 
ON document_chunks (chunk_position);

-- 9. 创建复合索引，优化混合检索的性能
CREATE INDEX IF NOT EXISTS idx_document_chunks_hybrid 
ON document_chunks (knowledge_base_id, chunk_position, token_count);

-- 10. 为content_hash创建唯一索引，避免重复内容
CREATE UNIQUE INDEX IF NOT EXISTS idx_document_chunks_content_hash 
ON document_chunks (content_hash);

-- 注释说明：
-- - idx_document_chunks_content_fts: 支持中文全文搜索
-- - idx_document_chunks_kb_content: 优化按知识库ID和内容查询
-- - idx_document_chunks_embedding_cosine: 向量相似性搜索索引
-- - idx_document_chunks_content_btree: 支持LIKE模糊查询
-- - idx_document_chunks_kb_embedding: 优化向量检索查询
-- - idx_document_chunks_token_count: 支持按token数量排序
-- - idx_document_chunks_embedding_partial: 部分向量索引，只对非空embedding
-- - idx_document_chunks_position: 支持按片段位置排序
-- - idx_document_chunks_hybrid: 混合检索复合索引
-- - idx_document_chunks_content_hash: 内容去重索引
