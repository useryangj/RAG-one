# Redis RAG缓存系统

## 概述

本项目已集成Redis缓存系统，实现了聊天历史记忆功能，让AI助手能够记住对话上下文，提供更智能的问答体验。

## 功能特性

### 🧠 智能记忆
- **会话管理**：每个聊天会话都有独立的ID，支持多轮对话
- **上下文缓存**：Redis缓存最近10轮对话，作为后续问答的上下文
- **自动会话创建**：首次提问时自动创建新会话
- **会话历史**：支持查看、加载和删除历史会话

### ⚡ 高性能
- **Redis缓存**：快速访问聊天历史，提升响应速度
- **智能清理**：自动清理过期会话，节省存储空间
- **并发支持**：支持多用户同时使用不同会话

### 🔒 安全可靠
- **用户隔离**：每个用户只能访问自己的会话
- **权限验证**：严格的会话权限控制
- **数据持久化**：重要数据同时保存到数据库

## 技术架构

```
用户问题 → 会话ID → Redis缓存(历史上下文) → 知识库检索 → 上下文融合 → LLM生成 → 缓存更新
```

### 核心组件

1. **ChatCacheService** - Redis缓存服务
2. **ChatSession** - 会话数据模型
3. **ChatMessage** - 消息数据模型
4. **RagService** - 增强的RAG服务
5. **RagController** - 会话管理API

## 配置说明

### Redis配置

在 `application.yml` 中配置Redis连接：

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: ${REDIS_DATABASE:0}
      timeout: 2000ms

app:
  chat:
    cache:
      max-conversation-turns: 10  # 每个会话最多缓存的对话轮数
      ttl-hours: 24               # 缓存过期时间（小时）
      enabled: true               # 是否启用聊天历史缓存
```

### 环境变量

```bash
# Redis配置
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=
export REDIS_DATABASE=0
```

## 快速开始

### 1. 启动Redis

```bash
# 使用提供的脚本
./start-redis.sh

# 或手动启动
redis-server --daemonize yes --port 6379
```

### 2. 启动应用

```bash
# 启动后端
./start-dev.sh

# 启动前端
cd frontend
npm start
```

### 3. 使用会话功能

1. **创建新会话**：点击"新会话"按钮
2. **查看会话历史**：点击"更多" → "会话历史"
3. **加载历史会话**：在会话历史中点击"加载"
4. **删除会话**：在会话历史中点击"删除"

## API接口

### 会话管理

```http
# 创建会话
POST /api/rag/session/create
Content-Type: multipart/form-data
knowledgeBaseId: 1

# 获取会话信息
GET /api/rag/session/{sessionId}

# 获取用户会话列表
GET /api/rag/sessions

# 删除会话
DELETE /api/rag/session/{sessionId}
```

### 问答接口

```http
# 带会话的问答
POST /api/rag/ask
Content-Type: multipart/form-data
question: 你的问题
knowledgeBaseId: 1
sessionId: session-uuid  # 可选，不提供会自动创建

# 获取聊天历史
GET /api/rag/history?sessionId=session-uuid
```

## 数据结构

### ChatSession (Redis缓存)

```json
{
  "sessionId": "uuid",
  "userId": 1,
  "knowledgeBaseId": 1,
  "knowledgeBaseName": "知识库名称",
  "createdAt": "2024-01-01T00:00:00",
  "lastActiveAt": "2024-01-01T00:00:00",
  "messages": [
    {
      "role": "user",
      "content": "用户问题",
      "timestamp": "2024-01-01T00:00:00",
      "contextChunks": "相关文档片段JSON"
    }
  ],
  "messageCount": 2
}
```

### ChatHistory (数据库)

```sql
CREATE TABLE chat_histories (
    id BIGINT PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL,
    knowledge_base_id BIGINT,
    user_message TEXT NOT NULL,
    assistant_response TEXT NOT NULL,
    context_chunks JSONB,
    response_time_ms BIGINT,
    created_at TIMESTAMP NOT NULL
);
```

## 缓存策略

### 会话缓存
- **键格式**：`chat:session:{sessionId}`
- **过期时间**：24小时
- **内容**：完整的会话信息

### 用户会话列表
- **键格式**：`chat:user:{userId}`
- **过期时间**：24小时
- **内容**：用户的所有会话ID列表

### 消息限制
- 每个会话最多保存10轮对话（20条消息）
- 超出限制时自动删除最旧的消息
- 保持会话的连续性和相关性

## 性能优化

### Redis优化
- 使用连接池管理Redis连接
- 合理设置超时时间
- 定期清理过期数据

### 内存管理
- 限制单会话消息数量
- 自动清理过期会话
- 智能上下文截断

## 监控和调试

### 日志配置

```yaml
logging:
  level:
    com.example.ragone.service.ChatCacheService: DEBUG
    com.example.ragone.service.RagService: DEBUG
```

### Redis监控

```bash
# 查看Redis状态
redis-cli info

# 查看缓存键
redis-cli keys "chat:*"

# 查看会话内容
redis-cli get "chat:session:your-session-id"
```

## 故障排除

### 常见问题

1. **Redis连接失败**
   - 检查Redis服务是否启动
   - 验证连接配置
   - 检查防火墙设置

2. **会话丢失**
   - 检查Redis内存使用
   - 验证TTL设置
   - 查看Redis日志

3. **性能问题**
   - 调整连接池配置
   - 优化缓存策略
   - 监控Redis性能

### 调试命令

```bash
# 检查Redis连接
redis-cli ping

# 查看内存使用
redis-cli info memory

# 查看连接数
redis-cli info clients

# 清空所有缓存
redis-cli flushall
```

## 扩展功能

### 未来计划
- [ ] 会话搜索功能
- [ ] 会话导出/导入
- [ ] 多语言支持
- [ ] 会话分析统计
- [ ] 智能会话推荐

### 自定义配置
- 调整缓存大小限制
- 修改过期时间策略
- 添加自定义缓存键前缀
- 实现分布式缓存

## 贡献指南

欢迎提交Issue和Pull Request来改进Redis RAG缓存系统！

## 许可证

本项目采用MIT许可证。

